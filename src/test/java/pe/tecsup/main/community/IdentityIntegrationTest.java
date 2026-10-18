package pe.tecsup.main.community;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:identity_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_identity","main.admin.password=SyntheticIdentity2026!"})
@AutoConfigureMockMvc @Transactional
class IdentityIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired IdentityService users;@Autowired AcademicService academic;
 final String admin="admin_identity";
 @BeforeEach void accounts(){
  users.create(admin,new IdentityService.NewAccount("familia_test","Familiar de prueba","FAMILIAR","SyntheticFamily2026!",null,null));
  users.create(admin,new IdentityService.NewAccount("profe_test","Docente de prueba","DOCENTE","SyntheticTeacher2026!",null,1L));
 }
 @Test void studentCannotGrantFamilyAccessOrCreateAccounts() throws Exception{
  mvc.perform(post("/api/admin/v1/guardians").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json")
   .content("{\"guardian\":\"familia_test\",\"studentId\":1}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/v1/users").with(user("familia_test").roles("FAMILIAR")).with(csrf()).contentType("application/json").content("{}"))
   .andExpect(status().isForbidden());
  assertTrue(academic.agenda("familia_test").courses().isEmpty());
 }
 @Test void verifiedFamilyLinkScopesAndRevokesAcademicData() throws Exception{
  users.verifyGuardian(admin,"familia_test",1);
  mvc.perform(get("/api/v1/agenda").with(user("familia_test").roles("FAMILIAR")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.courses",hasSize(2))).andExpect(jsonPath("$.students[0].name").value("Alex Demo"));
  users.revokeGuardian(admin,"familia_test",1);
  assertTrue(academic.agenda("familia_test").courses().isEmpty());
  assertTrue(academic.agenda("familia_test").students().isEmpty());
 }
 @Test void teacherSeesOnlyAssignedCoursesAndRoleProfilesAreReal() throws Exception{
  assertEquals(2,academic.agenda("profe_test").courses().size());
  assertEquals(1,academic.agenda("otro").courses().size());
  mvc.perform(get("/api/perfil").with(user("profe_test").roles("DOCENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("Docente"));
  mvc.perform(get("/api/actividades/pendientes").with(user("familia_test").roles("FAMILIAR"))).andExpect(status().isForbidden());
 }
 @Test void persistedAccountCanLoginAndPasswordIsNeverReturned() throws Exception{
  var login=mvc.perform(formLogin("/login").user("familia_test").password("SyntheticFamily2026!")).andExpect(status().is3xxRedirection()).andReturn();
  var session=(MockHttpSession)login.getRequest().getSession(false);
  mvc.perform(get("/api/perfil").session(session)).andExpect(jsonPath("$.nombre").value("Familiar de prueba"))
   .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
  assertNotEquals("SyntheticFamily2026!",users.loadUserByUsername("familia_test").getPassword());
 }
 @Test void preferencesAreScopedRequireCsrfAndValidateAudience() throws Exception{
  mvc.perform(put("/api/v1/preferences").with(user("alumno").roles("ALUMNO")).contentType("application/json")
   .content("{\"darkTheme\":true,\"notifications\":false,\"statusAudience\":\"PRIVATE\"}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/preferences").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json")
   .content("{\"darkTheme\":true,\"notifications\":false,\"statusAudience\":\"PRIVATE\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.darkTheme").value(true));
  assertFalse(users.preferences("otro").darkTheme());
  mvc.perform(put("/api/v1/preferences").with(user("alumno").roles("ALUMNO")).with(csrf()).contentType("application/json")
   .content("{\"statusAudience\":\"EVERYONE\"}")).andExpect(status().isBadRequest());
 }
 @Test void contactsBlockBothDirectionsAndReportsArePrivate() throws Exception{
  users.approveContact(admin,"alumno","otro");
  users.contactPreference("alumno","otro",true,true);
  assertTrue(users.blocked("otro","alumno"));assertTrue(users.contacts("alumno").getFirst().muted());
  users.report("alumno","otro","Solicitud de revisión de contacto.");
  mvc.perform(get("/api/admin/v1/reports").with(user("otro").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/v1/reports").with(user(admin).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(1)));
  assertThrows(DomainException.class,()->users.contactPreference("vacio","otro",false,true));
 }
 @Test void passwordRecoveryExpiresExistingSession() throws Exception{
  var result=mvc.perform(formLogin("/login").user("familia_test").password("SyntheticFamily2026!")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  users.resetPassword(admin,"familia_test","SyntheticReplacement2026!");
  mvc.perform(get("/api/v1/preferences").session(session)).andExpect(status().isUnauthorized());
  mvc.perform(formLogin("/login").user("familia_test").password("SyntheticFamily2026!")).andExpect(redirectedUrl("/login.html?error"));
  mvc.perform(formLogin("/login").user("familia_test").password("SyntheticReplacement2026!")).andExpect(redirectedUrl("/"));
 }
 @Test void disablingAccountImmediatelyRevokesSession() throws Exception{
  var result=mvc.perform(formLogin("/login").user("familia_test").password("SyntheticFamily2026!")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);users.disable(admin,"familia_test");
  mvc.perform(get("/api/v1/agenda").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void timetableRejectsOverlapAndOnlyAppearsToAuthorizedAccounts(){
  academic.createSlot(admin,new AcademicService.NewSlot(1L,1,"09:00","10:00","Aula T01"));
  assertEquals(1,academic.agenda("alumno").timetable().size());assertTrue(academic.agenda("otro").timetable().isEmpty());
  assertThrows(DomainException.class,()->academic.createSlot(admin,new AcademicService.NewSlot(2L,1,"09:30","10:30","Aula T02")));
  assertThrows(DomainException.class,()->academic.createSlot("alumno",new AcademicService.NewSlot(1L,2,"09:00","10:00","T01")));
  assertThrows(DomainException.class,()->academic.createSlot(admin,new AcademicService.NewSlot(1L,8,"10:00","09:00","T01")));
 }
 @Test void invalidRolesAndAcademicIdentityTheftAreRejected(){
  assertThrows(DomainException.class,()->users.create(admin,new IdentityService.NewAccount("fraude","Prueba","ALUMNO","SyntheticPassword2026!",1L,null)));
  assertThrows(DomainException.class,()->users.create(admin,new IdentityService.NewAccount("escalado","Prueba","ADMIN","SyntheticPassword2026!",null,null)));
  assertThrows(DomainException.class,()->users.create(admin,new IdentityService.NewAccount("debil","Prueba","FAMILIAR","short",null,null)));
 }
}
