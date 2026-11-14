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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc
class ActividadIntegrationTest {
 @Autowired MockMvc mvc;
 @Test void anonymousRejected()throws Exception{mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isUnauthorized());}
 @Test void faviconIsPublic()throws Exception{mvc.perform(get("/favicon.ico")).andExpect(status().isOk()).andExpect(content().contentType("image/svg+xml"));}
 @Test void loginThemeIsPublic()throws Exception{mvc.perform(get("/login-modern.css")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/css"));}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void listIsScopedAndExcludesCompleted()throws Exception{
  mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)))
   .andExpect(jsonPath("$[*].id",contains(3,1,2))).andExpect(jsonPath("$[*].completada",everyItem(is(false))));
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void allActivitiesIncludesCompletedHistory()throws Exception{
  mvc.perform(get("/api/actividades")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(4)))
   .andExpect(jsonPath("$[0].id").value(4)).andExpect(jsonPath("$[0].completada").value(true));
 }
 @Test @Transactional @WithMockUser(username="alumno",roles="ALUMNO") void submissionCanBeCreatedAndRejectsStaleUpdate()throws Exception{
  mvc.perform(put("/api/actividades/1/entrega").with(csrf()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"note\":\"Informe revisado\",\"evidenceUrl\":\"https://example.edu/evidencia\",\"expectedRevision\":0}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.submitted").value(true)).andExpect(jsonPath("$.revision").value(1))
   .andExpect(jsonPath("$.note").value("Informe revisado"));
  mvc.perform(get("/api/actividades/1/entrega")).andExpect(status().isOk()).andExpect(jsonPath("$.evidenceUrl").value("https://example.edu/evidencia"));
  mvc.perform(put("/api/actividades/1/entrega").with(csrf()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"note\":\"Versión antigua\",\"expectedRevision\":0}"))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("STALE_SUBMISSION"));
 }
 @Test @Transactional @WithMockUser(username="alumno",roles="ALUMNO") void draftDoesNotCompleteActivityAndCanBeSubmitted()throws Exception{
  mvc.perform(put("/api/actividades/2/entrega").with(csrf()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"note\":\"Avance en proceso\",\"status\":\"DRAFT\",\"expectedRevision\":0}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.submitted").value(false))
   .andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.revision").value(1));
  mvc.perform(get("/api/actividades/2")).andExpect(status().isOk()).andExpect(jsonPath("$.completada").value(false));
  mvc.perform(put("/api/actividades/2/entrega").with(csrf()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"note\":\"Versión final\",\"status\":\"SUBMITTED\",\"expectedRevision\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.submitted").value(true)).andExpect(jsonPath("$.revision").value(2));
  mvc.perform(get("/api/actividades/2")).andExpect(status().isOk()).andExpect(jsonPath("$.completada").value(true));
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void invalidSubmissionIsStableAndForeignActivityStaysHidden()throws Exception{
  mvc.perform(put("/api/actividades/1/entrega").with(csrf()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"note\":\"\",\"evidenceUrl\":\"javascript:alert(1)\",\"expectedRevision\":0}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  mvc.perform(get("/api/actividades/5/entrega")).andExpect(status().isNotFound());
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void detailHasRequiredFields()throws Exception{
  mvc.perform(get("/api/actividades/1")).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Primer avance de MAIN"))
   .andExpect(jsonPath("$.descripcion",not(emptyString()))).andExpect(jsonPath("$.curso",not(emptyString()))).andExpect(jsonPath("$.docente",not(emptyString()))).andExpect(jsonPath("$.fechaLimite").value("2026-10-06T15:30:00"));
 }
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void otherCourseIsHidden()throws Exception{mvc.perform(get("/api/actividades/5")).andExpect(status().isNotFound());}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void unknownIsHidden()throws Exception{mvc.perform(get("/api/actividades/999")).andExpect(status().isNotFound())
  .andExpect(jsonPath("$.error.code").value("NOT_FOUND")).andExpect(jsonPath("$.error.message").value("El contenido no existe o no está disponible para tu cuenta."));}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void malformedIdHasStableErrorContract()throws Exception{mvc.perform(get("/api/actividades/no-es-un-id"))
  .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));}
 @Test @WithMockUser(username="vacio",roles="ALUMNO") void emptyState()throws Exception{mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isOk()).andExpect(content().json("[]"));}
 @Test @WithMockUser(username="otro",roles="ALUMNO") void secondUserSeesOnlyOwnCourse()throws Exception{
 mvc.perform(get("/api/actividades/pendientes")).andExpect(jsonPath("$",hasSize(1))).andExpect(jsonPath("$[0].id").value(5));
 mvc.perform(get("/api/actividades/1")).andExpect(status().isNotFound());
 }
 @Test void correctCredentialsLogin()throws Exception{mvc.perform(formLogin("/login").user("alumno").password("MainDemo2026!")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));}
 @Test void wrongPasswordRejected()throws Exception{mvc.perform(formLogin("/login").user("alumno").password("wrong")).andExpect(redirectedUrl("/login.html?error"));}
 @Test @WithMockUser(username="alumno",roles="ALUMNO") void logoutRequiresCsrf()throws Exception{mvc.perform(post("/logout")).andExpect(status().isForbidden());}
}
