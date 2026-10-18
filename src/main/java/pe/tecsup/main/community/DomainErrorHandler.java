package pe.tecsup.main.community;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice(basePackages="pe.tecsup.main.community")
public class DomainErrorHandler {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<?> domain(DomainException ex){return ResponseEntity.status(ex.status).body(Map.of("error",Map.of("code",ex.code,"message",ex.getMessage())));}
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> malformed(Exception ex){return ResponseEntity.badRequest().body(Map.of("error",Map.of("code","INVALID_REQUEST","message","JSON o parámetros inválidos.")));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(Exception ex){return ResponseEntity.status(409).body(Map.of("error",Map.of("code","CONFLICT","message","Conflicto de identificadores o relaciones. No se modificaron los datos.")));}
}
