package pe.tecsup.main.community;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:academic_admin;DB_CLOSE_DELAY=-1","main.admin.username=admin_academic","main.admin.password=SyntheticAcademic2026!"})
@AutoConfigureMockMvc @Transactional
class AcademicAdministrationTest {
 @Autowired AcademicAdministration administration;@Autowired IdentityService users;@Autowired AcademicService academic;
 @Autowired JdbcTemplate db;@Autowired MockMvc mvc;@Autowired ObjectMapper json;
 final String admin="admin_academic";
 @BeforeEach void setup(){users.create(admin,new IdentityService.NewAccount("teacher_academic","Docente sintético","DOCENTE","SyntheticTeacher2026!",null,91L));}
 AcademicAdministration.Input input(int version){return new AcademicAdministration.Input(90,"Curso de pruebas","teacher_academic",version<0?null:version);}
 @Test void onlyAdministratorCanCreateCoursesOrReadPrivateAudit()throws Exception{
  mvc.perform(post("/api/admin/v1/courses").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input(-1)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/v1/audit").with(user("teacher_academic").roles("DOCENTE"))).andExpect(status().isForbidden());
  assertThrows(DomainException.class,()->administration.save("alumno",90,input(-1),true));
 }
 @Test void csrfAndInvalidPaginationAreRejected()throws Exception{
  mvc.perform(post("/api/admin/v1/courses").with(user(admin).roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(input(-1)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/v1/audit?limit=101").with(user(admin).roles("ADMIN"))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/admin/v1/courses?offset=-1").with(user(admin).roles("ADMIN"))).andExpect(status().isBadRequest());
 }
 @Test void adminCreatesCourseAndEnrollmentChangesStudentAndTeacherViews()throws Exception{
  mvc.perform(post("/api/admin/v1/courses").with(user(admin).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input(-1))))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0));
  assertTrue(academic.agenda("teacher_academic").courses().stream().anyMatch(c->c.id()==90));
  assertTrue(academic.agenda("alumno").courses().stream().noneMatch(c->c.id()==90));
  administration.enroll(admin,90,"alumno",true);
  assertTrue(academic.agenda("alumno").courses().stream().anyMatch(c->c.id()==90));
  assertEquals("alumno",administration.students(admin,90,0,20).getFirst().username());
  administration.enroll(admin,90,"alumno",false);
  assertTrue(academic.agenda("alumno").courses().stream().noneMatch(c->c.id()==90));
 }
 @Test void reEnrollmentIsIdempotentAndAuditContainsNoDuplicateOrSecret(){
  administration.save(admin,90,input(-1),true);administration.enroll(admin,90,"alumno",true);administration.enroll(admin,90,"alumno",true);
  var events=administration.events(admin,0,100);
  assertEquals(1,events.stream().filter(e->e.action().equals("ENROLLMENT_GRANTED")).count());
  assertTrue(events.stream().noneMatch(e->e.subject().contains("Synthetic")));
  assertTrue(events.stream().allMatch(e->e.actor().equals(admin)));
 }
 @Test void staleCourseVersionDoesNotOverwriteNewerChanges(){
  administration.save(admin,90,input(-1),true);assertEquals(1,administration.save(admin,90,input(0),false).version());
  var conflict=assertThrows(DomainException.class,()->administration.save(admin,90,new AcademicAdministration.Input(90,"Viejo","teacher_academic",0),false));
  assertEquals(409,conflict.status.value());assertEquals("Curso de pruebas",administration.courses(admin,0,100).stream().filter(c->c.id()==90).findFirst().orElseThrow().name());
 }
 @Test void wrongRolesAndDisabledStudentsCannotBeAssigned(){
  assertThrows(DomainException.class,()->administration.save(admin,90,new AcademicAdministration.Input(90,"Prueba","alumno",null),true));
  administration.save(admin,90,input(-1),true);
  assertThrows(DomainException.class,()->administration.enroll(admin,90,"teacher_academic",true));
  users.disable(admin,"vacio");assertThrows(DomainException.class,()->administration.enroll(admin,90,"vacio",true));
 }
 @Test void changingTeacherRevokesPriorTeachersCourseAccess(){
  administration.save(admin,90,input(-1),true);
  users.create(admin,new IdentityService.NewAccount("replacement_academic","Otro docente","DOCENTE","SyntheticTeacher2026!",null,92L));
  administration.save(admin,90,new AcademicAdministration.Input(90,"Reasignado","replacement_academic",0),false);
  assertTrue(academic.agenda("teacher_academic").courses().isEmpty());assertEquals(90,academic.agenda("replacement_academic").courses().getFirst().id());
 }
 @Test void teacherReassignmentRequiresRemovingExistingTimetable(){
  administration.save(admin,90,input(-1),true);academic.createSlot(admin,new AcademicService.NewSlot(90,1,"08:00","09:00","Aula sintética"));
  users.create(admin,new IdentityService.NewAccount("replacement_academic","Otro docente","DOCENTE","SyntheticTeacher2026!",null,92L));
  assertThrows(DomainException.class,()->administration.save(admin,90,new AcademicAdministration.Input(90,"Curso","replacement_academic",0),false));
 }
}
