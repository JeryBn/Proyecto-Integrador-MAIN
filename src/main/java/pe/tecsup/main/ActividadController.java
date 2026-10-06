package pe.tecsup.main;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
@RestController @RequestMapping("/api")
public class ActividadController {
 private final ActividadService service;
 public ActividadController(ActividadService service){this.service=service;}
 @GetMapping("/perfil") public Map<String,String> perfil(Principal p){return service.perfil(p.getName());}
 @GetMapping("/actividades/pendientes") public List<ActividadService.Actividad> pendientes(Principal p){return service.pendientes(p.getName());}
 @GetMapping("/actividades/{id}") public ActividadService.Actividad detalle(Principal p,@PathVariable long id){return service.detalle(p.getName(),id);}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken t){return Map.of("parameterName",t.getParameterName(),"token",t.getToken());}
}
