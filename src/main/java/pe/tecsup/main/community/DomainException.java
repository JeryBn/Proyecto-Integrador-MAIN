package pe.tecsup.main.community;
import org.springframework.http.HttpStatus;
public class DomainException extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public DomainException(HttpStatus status,String code,String message){super(message);this.status=status;this.code=code;}
    public static DomainException invalid(String message){return new DomainException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",message);}
    public static DomainException denied(){return new DomainException(HttpStatus.FORBIDDEN,"FORBIDDEN","No tienes permiso para esta operación.");}
    public static DomainException missing(){return new DomainException(HttpStatus.NOT_FOUND,"NOT_FOUND","Recurso no disponible.");}
}
