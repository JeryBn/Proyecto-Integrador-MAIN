package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SchoolPublicationController {
 private final SchoolPublicationService service;
 public SchoolPublicationController(SchoolPublicationService service){this.service=service;}
 private ResponseEntity<?> data(Object value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
 @GetMapping("/teaching/publication-scopes") public ResponseEntity<?> scopes(Principal p){return data(service.scopes(p.getName()));}
 @GetMapping("/teaching/publications") public ResponseEntity<?> teaching(Principal p){return data(service.teaching(p.getName()));}
 @PostMapping("/teaching/publications") public ResponseEntity<?> publish(Principal p,@RequestBody SchoolPublicationService.Input input){return data(service.publish(p.getName(),input));}
 @GetMapping("/family/students/{student}/publications") public ResponseEntity<?> family(Principal p,@PathVariable long student){return data(service.family(p.getName(),student));}
 @PostMapping("/family/students/{student}/publications/{id}/read") public ResponseEntity<?> read(Principal p,@PathVariable long student,@PathVariable String id){service.read(p.getName(),student,id);return data(java.util.Map.of());}
 @GetMapping("/school/publications") public ResponseEntity<?> own(Principal p){return data(service.own(p.getName()));}
}
