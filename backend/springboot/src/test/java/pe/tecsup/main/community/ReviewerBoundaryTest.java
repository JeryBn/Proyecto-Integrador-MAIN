package pe.tecsup.main.community;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:reviewer_boundary;DB_CLOSE_DELAY=-1","main.admin.username=reviewer_boundary_admin","main.admin.password=SyntheticReviewerBoundary2026!"})
@AutoConfigureMockMvc @Transactional
class ReviewerBoundaryTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired IdentityService users;
 @BeforeEach void fixture(){db.update("INSERT INTO main_user(username,display_name,role,password_hash,enabled) VALUES('limited_reviewer','Revisor sintético','REVIEWER','not-a-real-password-hash',TRUE)");}
 @Test void reviewerCannotReadStudentDataOrLegacyAdminModule()throws Exception{
  for(String path:new String[]{"/api/perfil","/api/v1/contacts","/api/v1/appeals","/api/v1/statuses","/api/actividades/pendientes","/api/admin/v1/users"})mvc.perform(get(path).with(user("limited_reviewer").roles("REVIEWER"))).andExpect(status().isForbidden());
 }
 @Test void normalAccountModuleCannotGrantReviewerOrResetIt(){
  String admin="reviewer_boundary_admin";
  assertThrows(DomainException.class,()->users.create(admin,new IdentityService.NewAccount("escalation","Escalación","REVIEWER","SyntheticReviewer2026!",null,null)));
  assertThrows(DomainException.class,()->users.resetPassword(admin,"limited_reviewer","SyntheticAnother2026!"));
  assertThrows(DomainException.class,()->users.disable(admin,"limited_reviewer"));
  assertThrows(DomainException.class,()->users.approveContact(admin,"limited_reviewer","alumno"));
 }
}
