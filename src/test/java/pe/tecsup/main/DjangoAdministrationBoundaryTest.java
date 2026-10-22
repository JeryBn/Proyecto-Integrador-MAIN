package pe.tecsup.main;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="main.admin.legacy-enabled=false") @AutoConfigureMockMvc
class DjangoAdministrationBoundaryTest {
 @Autowired MockMvc mvc;
 @Test void springAdministrativeApiIsClosedWhenDjangoOwnsAdministration() throws Exception {
  mvc.perform(get("/api/admin/csrf")).andExpect(status().isGone());
  mvc.perform(post("/api/admin/v1/users").with(user("legacy").roles("ADMIN")).with(csrf()).contentType("application/json").content("{}"))
   .andExpect(status().isGone());
 }
 @Test void springUserApiRemainsAvailable() throws Exception {
  mvc.perform(get("/api/perfil").with(user("alumno").roles("ALUMNO"))).andExpect(status().isOk());
 }
}
