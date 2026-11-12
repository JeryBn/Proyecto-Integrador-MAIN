package pe.tecsup.main.community;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import static org.junit.jupiter.api.Assertions.*;

class DomainErrorHandlerTest {
    private final DomainErrorHandler handler = new DomainErrorHandler();

    @Test void databaseFailureUsesRetryableSafeContract() {
        ResponseEntity<?> response = handler.databaseUnavailable(
            new DataAccessResourceFailureException("jdbc:secret-host/internal"));
        assertEquals(503, response.getStatusCode().value());
        assertError(response, "SERVICE_UNAVAILABLE", "No pudimos consultar la información. Intenta nuevamente.");
        assertFalse(response.getBody().toString().contains("secret-host"));
    }

    @Test void missingRequiredParameterRemainsAClientError() {
        ResponseEntity<?> response = handler.malformed(
            new MissingServletRequestParameterException("clientKey", "String"));
        assertEquals(400, response.getStatusCode().value());
        assertError(response, "INVALID_REQUEST", "JSON o parámetros inválidos.");
    }

    @Test void unexpectedFailureDoesNotExposeTechnicalMessage() {
        ResponseEntity<?> response = handler.unexpected(new IllegalStateException("private implementation detail"));
        assertEquals(500, response.getStatusCode().value());
        assertError(response, "INTERNAL_ERROR", "Ocurrió un problema inesperado. Intenta nuevamente.");
        assertFalse(response.getBody().toString().contains("implementation detail"));
    }

    @SuppressWarnings("unchecked")
    private void assertError(ResponseEntity<?> response, String code, String message) {
        Map<String,Object> body = (Map<String,Object>) response.getBody();
        Map<String,Object> error = (Map<String,Object>) body.get("error");
        assertEquals(code, error.get("code"));
        assertEquals(message, error.get("message"));
    }
}
