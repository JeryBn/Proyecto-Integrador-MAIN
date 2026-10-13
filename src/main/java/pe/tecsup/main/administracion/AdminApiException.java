package pe.tecsup.main.administracion;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AdminApiException extends RuntimeException {
    final HttpStatus status;
    final String code;
    final Map<String, String> fields;

    public AdminApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public AdminApiException(HttpStatus status, String code, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = Map.copyOf(fields);
    }
}
