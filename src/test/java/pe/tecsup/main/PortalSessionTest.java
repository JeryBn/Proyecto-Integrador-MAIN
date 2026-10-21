package pe.tecsup.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Revisión asistida T05/T06: sesiones reales de formulario, sin simular autenticación. */
@SpringBootTest @AutoConfigureMockMvc
class PortalSessionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    record Token(MockHttpSession session, String name, String value) {}
    Token token(MockHttpSession session) throws Exception {
        var request = get("/login-token");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        return new Token((MockHttpSession) result.getRequest().getSession(),
                json.get("parameterName").asText(), json.get("token").asText());
    }
    MockHttpSession login(Token token) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").session(token.session())
                .param(token.name(), token.value()).param("username", "alumno").param("password", "MainDemo2026!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession();
    }

    @Test void expiredLoginPageNeedsFreshTokenAndThenCanAuthenticate() throws Exception {
        var old = token(null);
        old.session().invalidate();
        mvc.perform(post("/login").param(old.name(), old.value()).param("username", "alumno")
                .param("password", "MainDemo2026!")).andExpect(status().isForbidden());
        var current = login(token(null));
        mvc.perform(get("/api/actividades/pendientes").session(current)).andExpect(status().isOk());
    }

    @Test void tokenFromBeforeLoginCannotLogoutButRenewedTokenDoes() throws Exception {
        var before = token(null);
        var session = login(before);
        mvc.perform(post("/logout").session(session).param(before.name(), before.value()))
                .andExpect(status().isForbidden());
        var fresh = token(session);
        mvc.perform(post("/logout").session(fresh.session()).param(fresh.name(), fresh.value()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html?salida"));
        mvc.perform(get("/api/actividades/pendientes")).andExpect(status().isUnauthorized());
    }

    @Test void expiredSessionNeverReturnsPrivateDetail() throws Exception {
        var session = login(token(null));
        mvc.perform(get("/api/actividades/1").session(session)).andExpect(status().isOk());
        session.invalidate();
        mvc.perform(get("/api/actividades/1")).andExpect(status().isUnauthorized());
    }
}
