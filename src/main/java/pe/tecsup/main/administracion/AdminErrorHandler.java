package pe.tecsup.main.administracion;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes=AdminController.class)
public class AdminErrorHandler {
    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<?> error(AdminApiException ex) {
        return ResponseEntity.status(ex.status).body(Map.of("error",
            Map.of("code", ex.code, "message", ex.getMessage(), "fields", ex.fields)));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", Map.of("code", "INVALID_REQUEST", "message", "JSON, tipos o parámetros inválidos.")));
    }
}
