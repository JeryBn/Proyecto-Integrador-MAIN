package pe.tecsup.main.community;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/access")
public class AccountAccessController {
 private final AccountAccessService access;private final Clock clock;
 private record Window(Instant until,int count){}
 private final Map<String,Window> attempts=new ConcurrentHashMap<>();
 public AccountAccessController(AccountAccessService access,Clock clock){this.access=access;this.clock=clock;}
 public record Reset(String identifier){}
 public record Registration(String username,String name,String email,String password){@Override public String toString(){return "Registration[redacted]";}}
 public record Confirmation(String username,String code,String intent,String password){@Override public String toString(){return "Confirmation[redacted]";}}
 private synchronized void limit(HttpServletRequest request,String lane){
  var now=clock.instant();attempts.entrySet().removeIf(e->!e.getValue().until().isAfter(now));
  if(attempts.size()>10000)throw new DomainException(HttpStatus.TOO_MANY_REQUESTS,"ACCESS_LIMIT","Intenta más tarde.");
  String key=lane+":"+request.getRemoteAddr();var old=attempts.get(key);int count=old==null?0:old.count();
  if(count>=10)throw new DomainException(HttpStatus.TOO_MANY_REQUESTS,"ACCESS_LIMIT","Demasiados intentos. Espera 15 minutos.");
  attempts.put(key,new Window(old==null?now.plusSeconds(900):old.until(),count+1));
 }
 private ResponseEntity<?> message(String value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("message",value));}
 @PostMapping("/reset-request") public ResponseEntity<?> reset(@RequestBody Reset input,HttpServletRequest request){limit(request,"request");access.requestReset(input.identifier());return message("Si la cuenta tiene un correo verificado, recibirás un código de recuperación. Revisa también tu carpeta de spam.");}
 @PostMapping("/register-family") public ResponseEntity<?> register(@RequestBody Registration input,HttpServletRequest request){limit(request,"register");boolean sent=access.registerFamily(input.username(),input.name(),input.email(),input.password());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("emailSent",sent,"message",sent?"Revisa tu correo para activar la cuenta. El colegio deberá verificar por separado el vínculo con tus hijos.":"Solicitud registrada. El colegio verificará tu identidad y te entregará un código para activar tu cuenta. El vínculo con tus hijos se autoriza por separado."));}
 @PostMapping("/confirm") public ResponseEntity<?> confirm(@RequestBody Confirmation input,HttpServletRequest request){limit(request,"confirm");if(!Set.of("reset","activate").contains(Objects.toString(input.intent(),"")))throw DomainException.invalid("Operación inválida.");access.confirm(input.username(),input.code(),input.intent(),input.password());return message("Acceso actualizado. Ya puedes iniciar sesión con tu contraseña.");}
}
