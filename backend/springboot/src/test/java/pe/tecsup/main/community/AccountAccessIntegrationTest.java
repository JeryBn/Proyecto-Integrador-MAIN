package pe.tecsup.main.community;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:account_access;DB_CLOSE_DELAY=-1","main.demo.enabled=true","main.admin.username=access_admin","main.admin.password=SyntheticAccess2026!"})
@AutoConfigureMockMvc @Transactional
class AccountAccessIntegrationTest {
 @Autowired AccountAccessService access;@Autowired JdbcTemplate db;@Autowired PasswordEncoder encoder;@Autowired IdentityService users;@Autowired FamilySchoolService family;@Autowired MockMvc mvc;
 @MockBean AccountMail mail;
 private static final String PASSWORD="PersonalAccess2026!";
 @BeforeEach void configuredMail(){when(mail.configured()).thenReturn(true);}
 private String lastCode(){var capture=ArgumentCaptor.forClass(String.class);verify(mail,atLeastOnce()).send(anyString(),anyString(),capture.capture());return capture.getValue();}
 private void contact(String username,boolean verified){db.update("INSERT INTO account_contact(username,email,verified_at,created_at,updated_at) VALUES(?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",username,username+"@example.test",verified?Timestamp.valueOf(LocalDateTime.now()):null);}
 @Test void familyRegistrationHasOneRoleAndNoStudentAccess(){
  access.registerFamily("family_access","Familia de prueba","family@example.test",PASSWORD);
  assertEquals("FAMILIAR",users.account("family_access").role());assertFalse(users.account("family_access").enabled());
  assertThrows(DomainException.class,()->family.students("family_access"));
  String code=lastCode();access.confirm("family_access",code,"activate",PASSWORD);
  assertTrue(users.account("family_access").enabled());assertTrue(family.students("family_access").isEmpty());
  assertThrows(DomainException.class,()->family.catalog("family_access",1));
  assertNotNull(db.queryForObject("SELECT verified_at FROM account_contact WHERE username='family_access'",Timestamp.class));
  assertThrows(DomainException.class,()->access.confirm("family_access",code,"activate",PASSWORD));
 }
 @Test void recoveryStoresOnlyHashAndUpdatesExistingBcrypt(){
  contact("alumno",true);access.requestReset("alumno");String code=lastCode();
  assertEquals(AccountAccessService.digest(code),db.queryForObject("SELECT token_hash FROM account_access_token WHERE username='alumno'",String.class));
  String previous=db.queryForObject("SELECT password_hash FROM main_user WHERE username='alumno'",String.class);
  access.confirm("alumno",code,"reset",PASSWORD);
  String changed=db.queryForObject("SELECT password_hash FROM main_user WHERE username='alumno'",String.class);
  assertNotEquals(previous,changed);assertTrue(encoder.matches(PASSWORD,changed));
  assertThrows(DomainException.class,()->access.confirm("alumno",code,"reset",PASSWORD));
 }
 @Test void nonexistentUnverifiedAndDisabledAccountsReceiveNoReset(){
  contact("alumno",false);access.requestReset("unknown");access.requestReset("alumno");
  db.update("UPDATE account_contact SET verified_at=CURRENT_TIMESTAMP");db.update("UPDATE main_user SET enabled=FALSE WHERE username='alumno'");access.requestReset("alumno");
  verify(mail,never()).send(anyString(),anyString(),anyString());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM account_access_token",Integer.class));
 }
 @Test void expiredWrongOwnerAndWrongPurposeCodesFail(){
  contact("alumno",true);access.requestReset("alumno");String code=lastCode();
  assertThrows(DomainException.class,()->access.confirm("otro",code,"reset",PASSWORD));
  assertThrows(DomainException.class,()->access.confirm("alumno",code,"activate",PASSWORD));
  db.update("UPDATE account_access_token SET expires_at=?",Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
  assertThrows(DomainException.class,()->access.confirm("alumno",code,"reset",PASSWORD));
 }
 @Test void credentialChangesInvalidatePreviouslyIssuedCodes(){
  contact("alumno",true);access.requestReset("alumno");String code=lastCode();users.resetPassword("access_admin","alumno","AnotherPassword2026!");
  assertThrows(DomainException.class,()->access.confirm("alumno",code,"reset",PASSWORD));
 }
 @Test void duplicateAccountsAndShortPasswordsAreRejected(){
  assertThrows(DomainException.class,()->access.registerFamily("alumno","Duplicado","duplicate@example.test",PASSWORD));
  assertThrows(DomainException.class,()->access.registerFamily("new_family","Familia","family@example.test","short"));
 }
 @Test void familyCanRequestSchoolActivationWithoutAnEmailServer(){
  when(mail.configured()).thenReturn(false);
  assertFalse(access.registerFamily("pending_family","Familia pendiente","pending@example.test",PASSWORD));
  assertFalse(users.account("pending_family").enabled());
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM guardian_link WHERE guardian='pending_family'",Integer.class));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM account_access_token WHERE username='pending_family'",Integer.class));
  verify(mail,never()).send(anyString(),anyString(),anyString());
 }
 @Test void recoveryRevokesPreviouslyAuthenticatedSessions()throws Exception{
  var session=(org.springframework.mock.web.MockHttpSession)mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin("/login").user("alumno").password("MainDemo2026!"))
    .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
  contact("alumno",true);access.requestReset("alumno");access.confirm("alumno",lastCode(),"reset",PASSWORD);
  mvc.perform(get("/api/perfil").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void publicEndpointsRequireCsrfAndReturnNoCode()throws Exception{
  mvc.perform(post("/api/access/reset-request").contentType("application/json").content("{\"identifier\":\"unknown\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/access/reset-request").with(csrf()).contentType("application/json").content("{\"identifier\":\"unknown\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.code").doesNotExist());
 }
 @Test void schoolInvitationUsesSameFingerprintWithoutClaimingEmailVerified(){
  contact("alumno",false);String hash=db.queryForObject("SELECT password_hash FROM main_user WHERE username='alumno'",String.class);
  String code="abcdefghijklmnopqrstuvwxyzABCDEF";
  db.update("INSERT INTO account_access_token(id,username,token_hash,purpose,delivery,credential_version,expires_at,created_at) VALUES('manual','alumno',?,'ACTIVATE','SCHOOL',?,?,CURRENT_TIMESTAMP)",AccountAccessService.digest(code),AccountAccessService.digest(hash+"\0ALUMNO"),Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
  access.confirm("alumno",code,"activate",PASSWORD);assertTrue(encoder.matches(PASSWORD,db.queryForObject("SELECT password_hash FROM main_user WHERE username='alumno'",String.class)));
  assertNull(db.queryForObject("SELECT verified_at FROM account_contact WHERE username='alumno'",Timestamp.class));
 }
}
