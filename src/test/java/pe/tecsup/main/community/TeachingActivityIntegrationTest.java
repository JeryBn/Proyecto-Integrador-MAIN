package pe.tecsup.main.community;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.tecsup.main.ActividadService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:teaching_activities_test;DB_CLOSE_DELAY=-1","main.admin.username=admin_teaching_activities","main.admin.password=SyntheticAdmin2026!"})
@AutoConfigureMockMvc @Transactional
class TeachingActivityIntegrationTest {
 @Autowired TeachingActivityService activities;@Autowired IdentityService users;@Autowired ActividadService studentActivities;@Autowired MockMvc mvc;
 @Autowired java.time.Clock clock;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;

 @Test void campusFeedHidesPendingAndOnlyAuthorCanWithdraw() throws Exception {
  String path="/api/v1/campus/feed";
  var student=user("alumno").roles("ALUMNO");
  var peer=user("otro").roles("ALUMNO");
  var teacher=user("teacher_activities").roles("DOCENTE");
  String body="{\"text\":\"Una publicación académica de prueba\"}";
  mvc.perform(post(path).with(teacher).with(csrf()).contentType("application/json").content(body))
      .andExpect(status().isForbidden());
  String result=mvc.perform(post(path).with(student).with(csrf()).contentType("application/json").content(body))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("PENDING"))
      .andReturn().getResponse().getContentAsString();
  String id=json.readTree(result).get("id").asText();
  mvc.perform(get(path).with(peer).param("view","for-you")).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get(path).with(student).param("view","mine")).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(delete(path+"/"+id).with(peer).with(csrf())).andExpect(status().isNotFound());
  jdbc.update("UPDATE campus_post SET state='PUBLISHED' WHERE id=?",id);
  mvc.perform(get(path).with(peer).param("view","for-you")).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(delete(path+"/"+id).with(student).with(csrf())).andExpect(status().isOk());
  mvc.perform(get(path).with(peer)).andExpect(jsonPath("$.length()").value(0));
 }

 @Test void contactCodeWorksAcrossCoursesAndRequiresRecipientApproval() throws Exception {
  var sender=user("alumno").roles("ALUMNO");
  var recipient=user("otro").roles("ALUMNO");
  String root="/api/v1/contact-codes";
  mvc.perform(post(root+"/mine").with(sender)).andExpect(status().isForbidden());
  String result=mvc.perform(post(root+"/mine").with(recipient).with(csrf()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.qr[0]").exists()).andReturn().getResponse().getContentAsString();
  String code=json.readTree(result).get("code").asText();
  assertTrue(code.matches("[0-9]{6}"));
  mvc.perform(post(root+"/mine").with(recipient).with(csrf())).andExpect(jsonPath("$.code").value(code));
  String request="{\"code\":\""+code+"\"}";
  mvc.perform(post(root+"/requests").with(sender).with(csrf()).contentType("application/json").content(request))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
  mvc.perform(get(root+"/requests").with(recipient)).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(post(root+"/requests").with(sender).with(csrf()).contentType("application/json").content(request))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
  assertFalse(users.contacts("alumno","otro"));
  mvc.perform(get(root+"/requests").with(recipient)).andExpect(jsonPath("$[0].sender").value("alumno"));
  mvc.perform(post(root+"/requests/alumno").with(recipient).with(csrf()).contentType("application/json")
      .content("{\"accept\":true}")).andExpect(status().isOk());
  assertTrue(users.contacts("alumno","otro"));
 }
 @Test void attendanceBatchIsAtomicAndSafeToRepeatAfterConnectionLoss() throws Exception {
  jdbc.update("INSERT INTO matricula(alumno_id,curso_id) VALUES(?,1)",users.active("otro").alumnoId());
  long first=users.active("alumno").alumnoId(), second=users.active("otro").alumnoId();
  String path="/api/v1/teaching/courses/1/attendance/"+java.time.LocalDate.now(clock);
  var teacher=user("teacher_activities").roles("DOCENTE");
  String body=json.writeValueAsString(new AttendanceController.Batch(java.util.List.of(
   new AttendanceController.Change(first,AttendanceController.Status.PRESENT,null),
   new AttendanceController.Change(second,AttendanceController.Status.LATE,null))));
  for(int i=0;i<2;i++)mvc.perform(put(path+"/batch").with(teacher).with(csrf()).contentType("application/json").content(body))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM course_attendance",Integer.class));
  String stale=json.writeValueAsString(new AttendanceController.Batch(java.util.List.of(
   new AttendanceController.Change(first,AttendanceController.Status.ABSENT,AttendanceController.Status.PRESENT),
   new AttendanceController.Change(second,AttendanceController.Status.ABSENT,null))));
  mvc.perform(put(path+"/batch").with(teacher).with(csrf()).contentType("application/json").content(stale))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("ATTENDANCE_CONFLICT"));
  assertEquals("PRESENT",jdbc.queryForObject("SELECT status FROM course_attendance WHERE student_id=?",String.class,first));
  mvc.perform(get("/api/v1/teaching/courses/1/attendance/2099-01-01").with(teacher)).andExpect(status().isBadRequest());
 }

 @Test void attendanceRequiresCourseOwnershipAndPersistsCorrections() throws Exception {
  var date=java.time.LocalDate.now(clock);
  String path="/api/v1/teaching/courses/1/attendance/"+date;
  var teacher=user("teacher_activities").roles("DOCENTE");
  long student=users.active("alumno").alumnoId();
  String body="{\"studentId\":"+student+",\"status\":\"PRESENT\"}";
  mvc.perform(get(path).with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/teaching/courses/3/attendance/"+date).with(teacher)).andExpect(status().isNotFound());
  mvc.perform(put(path).with(teacher).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put(path).with(teacher).with(csrf()).contentType("application/json").content(body))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PRESENT"));
  mvc.perform(put(path).with(teacher).with(csrf()).contentType("application/json").content(body.replace("PRESENT","LATE")))
      .andExpect(status().isOk());
  mvc.perform(get(path).with(teacher)).andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("LATE"));
 }
 @BeforeEach void setup(){users.create("admin_teaching_activities",new IdentityService.NewAccount("teacher_activities","Docente de actividades","DOCENTE","SyntheticTeacher2026!",null,1L));}
 @Test void assignedTeacherCreatesActivityVisibleToEnrolledStudent(){
  var created=activities.create("teacher_activities",1,new TeachingActivityService.Input("Proyecto final","Publica la evidencia revisada.","2027-12-20T18:00:00"));
  assertEquals("Proyecto final",created.title());assertEquals(1,created.enrolled());assertEquals(0,created.submitted());
  assertEquals(created.id(),studentActivities.detalle("alumno",created.id()).id());
 }
 @Test void submissionCountsDistinguishDraftsFromDeliveredWork(){
  var created=activities.create("teacher_activities",1,new TeachingActivityService.Input("Avance verificable","Incluye el enlace del repositorio.","2027-12-21T18:00:00"));
  studentActivities.guardarEntrega("alumno",created.id(),new ActividadService.EntregaRequest("Borrador","https://example.test/draft","DRAFT",0));
  var draft=activities.list("teacher_activities",1).stream().filter(item->item.id()==created.id()).findFirst().orElseThrow();
  assertEquals(1,draft.drafts());assertEquals(0,draft.submitted());
  studentActivities.guardarEntrega("alumno",created.id(),new ActividadService.EntregaRequest("Entrega","https://example.test/final","SUBMITTED",1));
  var submitted=activities.list("teacher_activities",1).stream().filter(item->item.id()==created.id()).findFirst().orElseThrow();
  assertEquals(0,submitted.drafts());assertEquals(1,submitted.submitted());
 }
 @Test void otherRolesAndUnassignedCoursesAreRejected(){
  assertThrows(DomainException.class,()->activities.list("alumno",1));
  assertThrows(DomainException.class,()->activities.create("teacher_activities",3,new TeachingActivityService.Input("No autorizado","No debe guardarse.","2027-12-20T18:00:00")));
 }
 @Test void invalidContentAndPastDeadlineAreRejected(){
  assertThrows(DomainException.class,()->activities.create("teacher_activities",1,new TeachingActivityService.Input(" ","Descripción","2027-12-20T18:00:00")));
  assertThrows(DomainException.class,()->activities.create("teacher_activities",1,new TeachingActivityService.Input("Actividad","Descripción","2020-01-01T10:00:00")));
  assertThrows(DomainException.class,()->activities.create("teacher_activities",1,new TeachingActivityService.Input("Actividad","Descripción","20/12/2027")));
 }
 @Test void endpointRequiresSessionCsrfAndAssignedTeacher()throws Exception{
  String body="{\"title\":\"Actividad API\",\"description\":\"Descripción segura\",\"dueAt\":\"2027-12-20T18:00:00\"}";
  mvc.perform(get("/api/v1/teaching/courses/1/activities")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/teaching/courses/1/activities").with(user("teacher_activities").roles("DOCENTE")).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/teaching/courses/1/activities").with(user("teacher_activities").roles("DOCENTE")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Actividad API"));
  mvc.perform(get("/api/v1/teaching/courses/1/activities").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
 }
}
