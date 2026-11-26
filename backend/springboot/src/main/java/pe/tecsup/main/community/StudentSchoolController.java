package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/school")
public class StudentSchoolController {
 private final FamilySchoolService service;
 public StudentSchoolController(FamilySchoolService service){this.service=service;}
 @GetMapping("/catalog") public ResponseEntity<?> catalog(Principal p){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownCatalog(p.getName()));}
 @GetMapping("/report") public ResponseEntity<?> report(Principal p,@RequestParam int yearId,@RequestParam String periodId,@RequestParam(required=false) Long courseId){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownReport(p.getName(),yearId,periodId,courseId));}
}
