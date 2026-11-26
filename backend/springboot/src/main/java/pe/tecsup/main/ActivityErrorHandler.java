package pe.tecsup.main;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = ActividadController.class)
public class ActivityErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ActivityErrorHandler.class);

    private ResponseEntity<?> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        if (error.getStatusCode().value() == 404) {
            return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "El contenido no existe o no está disponible para tu cuenta.");
        }
        log.warn("Controlled activity API failure: status={}", error.getStatusCode().value());
        return response(HttpStatus.BAD_REQUEST, "REQUEST_FAILED", "No se pudo completar la solicitud.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidParameter(MethodArgumentTypeMismatchException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revisa el identificador solicitado.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> databaseUnavailable(DataAccessException error) {
        log.error("Activity API database failure", error);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "No pudimos consultar la información. Intenta nuevamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception error) {
        log.error("Unexpected activity API failure", error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un problema inesperado. Intenta nuevamente.");
    }
}
