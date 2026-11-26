package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/v1")
public class AcademicAdminController {
 private final AcademicAdministration service;
 public AcademicAdminController(AcademicAdministration service){this.service=service;}
 public record Enrollment(String username,boolean enrolled){}
 @GetMapping("/courses") public List<AcademicAdministration.Course> courses(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.courses(p.getName(),offset,limit);}
 @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
 public AcademicAdministration.Course create(Principal p,@RequestBody AcademicAdministration.Input input){return service.save(p.getName(),input.id(),input,true);}
 @PutMapping("/courses/{id}") public AcademicAdministration.Course edit(Principal p,@PathVariable long id,@RequestBody AcademicAdministration.Input input){return service.save(p.getName(),id,input,false);}
 @GetMapping("/courses/{id}/students") public List<AcademicAdministration.Student> students(Principal p,@PathVariable long id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.students(p.getName(),id,offset,limit);}
 @PutMapping("/courses/{id}/students") public void student(Principal p,@PathVariable long id,@RequestBody Enrollment input){service.enroll(p.getName(),id,input.username(),input.enrolled());}
 @GetMapping("/audit") public List<AdministrativeAudit.Event> events(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.events(p.getName(),offset,limit);}
}
