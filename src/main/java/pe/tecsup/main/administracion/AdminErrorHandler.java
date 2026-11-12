package pe.tecsup.main.administracion;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes=AdminController.class)
public class AdminErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminErrorHandler.class);

    private ResponseEntity<?> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message)));
    }

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<?> error(AdminApiException ex) {
        return ResponseEntity.status(ex.status).body(Map.of("error",
            Map.of("code", ex.code, "message", ex.getMessage(), "fields", ex.fields)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        ServletRequestBindingException.class, BindException.class})
    public ResponseEntity<?> invalid(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "JSON, tipos o parámetros inválidos.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> databaseUnavailable(DataAccessException ex) {
        log.error("Administration API database failure", ex);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
            "No pudimos consultar la información. Intenta nuevamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        log.error("Unexpected administration API failure", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "Ocurrió un problema inesperado. Intenta nuevamente.");
    }
}
