package pe.tecsup.main.administracion;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:admin_tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate db;
    static final String TEST_PASSWORD = "SyntheticAdminTest2026!";
    static final String HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    @DynamicPropertySource static void credentials(DynamicPropertyRegistry properties) {
        properties.add("main.admin.username", () -> "admin_test");
        properties.add("main.admin.password-hash", () -> HASH);
    }
    Map<String,Object> payload() {
        return new LinkedHashMap<>(Map.of("id",100,"cursoId",1,"titulo","Nueva actividad",
            "descripcion","Instrucciones","fechaLimite","2026-11-20T15:30:00"));
    }
    org.springframework.test.web.servlet.ResultActions create(Map<String,Object> input) throws Exception {
        return mvc.perform(post("/api/admin/actividades").with(user("admin_test").roles("ADMIN"))
            .with(csrf()).contentType("application/json").content(mapper.writeValueAsString(input)));
    }
    @Test void creationIsReadableByEnrolledStudentAndHiddenFromOtherStudent() throws Exception {
        create(payload()).andExpect(status().isCreated()).andExpect(header().string("Location","/api/admin/actividades/100"));
        mvc.perform(get("/api/actividades/100").with(user("alumno").roles("ALUMNO")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Nueva actividad"))
            .andExpect(jsonPath("$.fechaLimite").value("2026-11-20T15:30:00"));
        mvc.perform(get("/api/actividades/pendientes").with(user("alumno").roles("ALUMNO")))
            .andExpect(jsonPath("$[*].id",hasItem(100)));
        mvc.perform(get("/api/actividades/100").with(user("otro").roles("ALUMNO"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/actividades/pendientes").with(user("otro").roles("ALUMNO")))
            .andExpect(jsonPath("$[*].id",not(hasItem(100))));
    }
    @Test void duplicateNeverOverwritesExistingData() throws Exception {
        var input=payload(); input.put("id",1);
        create(input).andExpect(status().isConflict());
        assertEquals("Primer avance de MAIN",db.queryForObject("SELECT titulo FROM actividad WHERE id=1",String.class));
    }
    @Test void anonymousRejected() throws Exception {
        mvc.perform(get("/api/admin/actividades")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/actividades").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(payload())))
            .andExpect(status().isUnauthorized());
    }
    @Test void studentCannotAdministerAndAdminCannotUseStudentApi() throws Exception {
        mvc.perform(get("/api/admin/cursos").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/actividades").with(user("alumno").roles("ALUMNO")).with(csrf())
            .contentType("application/json").content(mapper.writeValueAsString(payload()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/actividades/pendientes").with(user("admin_test").roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void csrfRequiredForMutations() throws Exception {
        mvc.perform(post("/api/admin/actividades").with(user("admin_test").roles("ADMIN"))
            .contentType("application/json").content(mapper.writeValueAsString(payload()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/logout").with(user("admin_test").roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void missingFieldsAndUnknownCourseRejected() throws Exception {
        create(Map.of()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.fechaLimite").exists());
        var input=payload(); input.put("cursoId",999);
        create(input).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.cursoId").exists());
    }
    @Test void invalidFieldsAndDatesRejected() throws Exception {
        for(var bad:Map.<String,Object>of("id",-1,"cursoId",0,"titulo"," ","descripcion","x".repeat(2001),"fechaLimite","2026-02-30T10:00:00").entrySet()) {
            var input=payload(); input.put(bad.getKey(),bad.getValue());
            create(input).andExpect(status().isBadRequest());
        }
        for(Object invalid:java.util.List.of(1.2,true,"100",9007199254740992L)) {
            var input=payload(); input.put("id",invalid); create(input).andExpect(status().isBadRequest());
        }
        var input=payload(); input.put("fechaLimite","2026-11-20T15:30:00Z"); create(input).andExpect(status().isBadRequest());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM actividad WHERE id=100",Integer.class));
    }
    @Test void unknownFieldsAndMalformedJsonRejected() throws Exception {
        var input=payload(); input.put("rol","ADMIN"); create(input).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/actividades").with(user("admin_test").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/actividades").with(user("admin_test").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("[]")).andExpect(status().isBadRequest());
    }
    @Test void listsArePaginatedAndOrdered() throws Exception {
        mvc.perform(get("/api/admin/actividades?limit=1&offset=0").with(user("admin_test").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(5)).andExpect(jsonPath("$.items[0].id").value(4));
        mvc.perform(get("/api/admin/cursos").with(user("admin_test").roles("ADMIN")))
            .andExpect(jsonPath("$.items",hasSize(3)));
        for(String query:java.util.List.of("limit=0","offset=-1","limit=101","offset=bad"))
            mvc.perform(get("/api/admin/actividades?"+query).with(user("admin_test").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void detailUnknownAndUnsupportedMethods() throws Exception {
        mvc.perform(get("/api/admin/actividades/999").with(user("admin_test").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/actividades/1").with(user("admin_test").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
    }
    @Test void loginAndLogoutPersistSessionAndRotateCsrf() throws Exception {
        var tokenResult=mvc.perform(get("/api/admin/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)tokenResult.getRequest().getSession(false);
        String oldToken=mapper.readTree(tokenResult.getResponse().getContentAsString()).get("token").asText();
        String oldId=session.getId();
        mvc.perform(post("/api/admin/login").session(session).param("username","admin_test").param("password",TEST_PASSWORD)
            .param("_csrf",oldToken)).andExpect(status().isOk());
        assertNotEquals(oldId,session.getId());
        mvc.perform(get("/api/admin/perfil").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value("admin_test"));
        mvc.perform(post("/api/admin/actividades").session(session).header("X-CSRF-TOKEN",oldToken)
            .contentType("application/json").content(mapper.writeValueAsString(payload()))).andExpect(status().isForbidden());
        var fresh=mvc.perform(get("/api/admin/csrf").session(session)).andReturn();
        String token=mapper.readTree(fresh.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/admin/logout").session(session).param("_csrf",token)).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/admin/perfil")).andExpect(status().isUnauthorized());
    }
    @Test void wrongCredentialsAndStudentLoginRejected() throws Exception {
        mvc.perform(post("/api/admin/login").with(csrf()).param("username","admin_test").param("password","wrong"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/login").with(csrf()).param("username","alumno").param("password","MainDemo2026!"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/login").param("username","admin_test").param("password",TEST_PASSWORD)).andExpect(status().isForbidden());
    }
}
