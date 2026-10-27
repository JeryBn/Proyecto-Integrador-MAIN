package pe.tecsup.main.community;
import java.time.*;import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:reminder_tests;DB_CLOSE_DELAY=-1",
 "main.admin.username=admin_reminder","main.admin.password=SyntheticReminder2026!","main.reminders.interval-ms=3600000"})
@AutoConfigureMockMvc @Transactional
class ReminderIntegrationTest {
 @Autowired ReminderService reminders;@Autowired NotificationService notices;@Autowired IdentityService users;
 @Autowired JdbcTemplate db;@Autowired Clock clock;@Autowired MockMvc mvc;
 @BeforeEach void fixture(){
  db.update("INSERT INTO actividad VALUES(9001,1,'Actividad sintética','Descripción privada',?)",Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(30)));
 }
 void enable(){reminders.settings("alumno",new ReminderService.Settings(true,60));}
 @Test void defaultOffOptInDeduplicatesAndExposesOnlyDestination()throws Exception{
  assertFalse(reminders.settings("alumno").enabled());reminders.dispatch();assertTrue(notices.list("alumno",0,100).isEmpty());
  enable();reminders.dispatch();reminders.dispatch();
  var item=notices.list("alumno",0,100).stream().filter(n->Long.valueOf(9001).equals(n.activityId())).findFirst().orElseThrow();
  assertEquals("REMINDER",item.kind());assertNull(item.conversationId());assertTrue(notices.pushVisible("alumno",item.id()));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM notification WHERE activity_id=9001",Integer.class));
  assertTrue(notices.list("otro",0,100).isEmpty());assertThrows(DomainException.class,()->notices.read("otro",item.id()));
  mvc.perform(get("/api/v1/notifications").with(user("alumno").roles("ALUMNO"))).andExpect(status().isOk())
   .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Descripción privada"))));
  notices.read("alumno",item.id());assertFalse(notices.pushVisible("alumno",item.id()));
 }
 @Test void handedInOrRevokedEnrollmentSuppressesQueuedReminder(){
  enable();reminders.dispatch();db.update("INSERT INTO entrega VALUES(1,9001,?)",Timestamp.valueOf(LocalDateTime.now(clock)));
  assertTrue(notices.list("alumno",0,100).stream().noneMatch(n->Long.valueOf(9001).equals(n.activityId())));
  db.update("DELETE FROM entrega WHERE actividad_id=9001");db.update("DELETE FROM matricula WHERE alumno_id=1 AND curso_id=1");
  assertTrue(notices.list("alumno",0,100).isEmpty());
 }
 @Test void changingDeadlineInvalidatesOldNoticeAndNewDeadlineCanNotify(){
  enable();reminders.dispatch();String previous=notices.list("alumno",0,100).stream().filter(n->Long.valueOf(9001).equals(n.activityId())).findFirst().orElseThrow().id();
  db.update("UPDATE actividad SET fecha_limite=? WHERE id=9001",Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(40)));
  assertFalse(notices.pushVisible("alumno",previous));reminders.dispatch();
  assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM notification WHERE activity_id=9001",Integer.class));
  assertEquals(1,notices.list("alumno",0,100).stream().filter(n->Long.valueOf(9001).equals(n.activityId())).count());
 }
 @Test void disabledGlobalOrReminderPreferenceSuppressesQueue(){
  enable();users.preferences("alumno",new IdentityService.Preferences(false,false,"CONTACTS"));reminders.dispatch();
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notification WHERE activity_id=9001",Integer.class));
  users.preferences("alumno",new IdentityService.Preferences(false,true,"CONTACTS"));reminders.dispatch();
  reminders.settings("alumno",new ReminderService.Settings(false,60));assertTrue(notices.list("alumno",0,100).isEmpty());
 }
 @Test void configuredWindowAndExpiredDeadlinesAreRespected(){
  reminders.settings("alumno",new ReminderService.Settings(true,15));reminders.dispatch();
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notification WHERE activity_id=9001",Integer.class));
  db.update("UPDATE actividad SET fecha_limite=? WHERE id=9001",Timestamp.valueOf(LocalDateTime.now(clock).minusMinutes(1)));reminders.dispatch();
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notification WHERE activity_id=9001",Integer.class));
 }
 @Test void preferencesRequireStudentCsrfAndValidWindow()throws Exception{
  assertThrows(DomainException.class,()->reminders.settings("admin_reminder"));
  assertThrows(DomainException.class,()->reminders.settings("alumno",new ReminderService.Settings(true,10080)));
  mvc.perform(put("/api/v1/reminders/preferences").with(user("alumno").roles("ALUMNO")).contentType("application/json").content("{\"enabled\":true,\"minutesBefore\":60}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/reminders/preferences").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"minutesBefore\":15}")).andExpect(status().isOk()).andExpect(jsonPath("$.minutesBefore").value(15));
 }
}
