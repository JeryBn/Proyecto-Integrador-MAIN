package pe.tecsup.main.community;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice(basePackages="pe.tecsup.main.community")
public class DomainErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(DomainErrorHandler.class);
    private ResponseEntity<?> response(int status,String code,String message){
        return ResponseEntity.status(status).body(Map.of("error",Map.of("code",code,"message",message)));
    }
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<?> domain(DomainException ex){return response(ex.status.value(),ex.code,ex.getMessage());}
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,
        ServletRequestBindingException.class,BindException.class})
    public ResponseEntity<?> malformed(Exception ex){return response(400,"INVALID_REQUEST","JSON o parámetros inválidos.");}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(Exception ex){return response(409,"CONFLICT","Conflicto de identificadores o relaciones. No se modificaron los datos.");}
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> databaseUnavailable(DataAccessException ex){
        log.error("Community API database failure",ex);
        return response(503,"SERVICE_UNAVAILABLE","No pudimos consultar la información. Intenta nuevamente.");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex){
        log.error("Unexpected community API failure",ex);
        return response(500,"INTERNAL_ERROR","Ocurrió un problema inesperado. Intenta nuevamente.");
    }
}
