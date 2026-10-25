package pe.tecsup.main.community;
import java.security.Principal;import java.util.Map;import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/push")
public class PushController {
 private final PushService service;public PushController(PushService service){this.service=service;}
 @GetMapping("/configuration") public Map<String,Boolean> configuration(){return Map.of("configured",service.configured());}
 @PostMapping("/devices") public Map<String,String> register(Principal p,HttpServletRequest request,@RequestBody PushService.Registration body){var session=request.getSession(false);if(session==null)throw DomainException.denied();return service.register(p.getName(),session.getId(),session.getMaxInactiveInterval(),body);}
 @DeleteMapping("/devices/{id}") public Map<String,Boolean> remove(Principal p,@PathVariable String id){service.remove(p.getName(),id);return Map.of("removed",true);}
}
