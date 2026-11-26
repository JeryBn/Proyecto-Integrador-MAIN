package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching/school")
public class TeachingSchoolController {
 private final TeachingSchoolService service;
 public TeachingSchoolController(TeachingSchoolService service){this.service=service;}
 private ResponseEntity<?> data(Object value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
 @GetMapping("/scopes") public ResponseEntity<?> scopes(Principal p){return data(service.scopes(p.getName()));}
 @GetMapping("/{assignment}/marks") public ResponseEntity<?> sheet(Principal p,@PathVariable String assignment,@RequestParam(required=false) String periodId){return data(service.sheet(p.getName(),assignment,periodId));}
 @PostMapping("/{assignment}/marks") public ResponseEntity<?> save(Principal p,@PathVariable String assignment,@RequestBody TeachingSchoolService.Input input){return data(service.save(p.getName(),assignment,input));}
}
