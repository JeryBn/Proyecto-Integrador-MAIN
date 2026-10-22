package pe.tecsup.main;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class SharedIdentitySessionTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
 MockHttpSession login() throws Exception {
  return (MockHttpSession)mvc.perform(formLogin("/login").user("alumno").password("MainDemo2026!"))
   .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
 }
 @Test void sqlPasswordChangeRevokesExistingAuthenticatedSession() throws Exception {
  var session=login();mvc.perform(get("/api/perfil").session(session)).andExpect(status().isOk());
  db.update("UPDATE main_user SET password_hash=? WHERE username='alumno'",new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("OtherSynthetic2026!"));
  mvc.perform(get("/api/perfil").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void sqlDisableRevokesExistingAuthenticatedSession() throws Exception {
  var session=login();db.update("UPDATE main_user SET enabled=FALSE WHERE username='alumno'");
  mvc.perform(get("/api/actividades/pendientes").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void sqlRoleChangeRevokesPreviousPrivileges() throws Exception {
  var session=login();db.update("UPDATE main_user SET role='FAMILIAR' WHERE username='alumno'");
  mvc.perform(get("/api/actividades/1").session(session)).andExpect(status().isUnauthorized());
 }
}
