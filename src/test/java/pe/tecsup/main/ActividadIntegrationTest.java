package pe.tecsup.main;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc
class ActividadIntegrationTest {
 @Autowired MockMvc mvc;
 @Test void anonymousRejected()throws Exception{mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void listIsScopedAndExcludesCompleted()throws Exception{
  mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)))
   .andExpect(jsonPath("$[*].id",contains(3,1,2))).andExpect(jsonPath("$[*].completada",everyItem(is(false))));
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void detailHasRequiredFields()throws Exception{
  mvc.perform(get("/api/actividades/1")).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Primer avance de MAIN"))
   .andExpect(jsonPath("$.descripcion",not(emptyString()))).andExpect(jsonPath("$.curso",not(emptyString()))).andExpect(jsonPath("$.docente",not(emptyString()))).andExpect(jsonPath("$.fechaLimite").value("2026-10-06T15:30:00"));
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void otherCourseIsHidden()throws Exception{mvc.perform(get("/api/actividades/5")).andExpect(status().isNotFound());}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void unknownIsHidden()throws Exception{mvc.perform(get("/api/actividades/999")).andExpect(status().isNotFound());}
 @Test @WithMockUser(username="vacio",roles="ALUMNO") void emptyState()throws Exception{mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isOk()).andExpect(content().json("[]"));}
 @Test @WithMockUser(username="otro",roles="ALUMNO") void secondUserSeesOnlyOwnCourse()throws Exception{
 mvc.perform(get("/api/actividades/pendientes")).andExpect(jsonPath("$",hasSize(1))).andExpect(jsonPath("$[0].id").value(5));
 mvc.perform(get("/api/actividades/1")).andExpect(status().isNotFound());
 }
 @Test void correctCredentialsLogin()throws Exception{mvc.perform(formLogin("/login").user("alumno").password("MainDemo2026!")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/api/perfil"));}
 @Test void wrongPasswordRejected()throws Exception{mvc.perform(formLogin("/login").user("alumno").password("wrong")).andExpect(redirectedUrl("/login?error"));}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void logoutRequiresCsrf()throws Exception{mvc.perform(post("/logout")).andExpect(status().isForbidden());}
}
