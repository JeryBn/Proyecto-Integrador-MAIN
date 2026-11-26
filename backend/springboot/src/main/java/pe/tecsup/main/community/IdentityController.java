package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class IdentityController {
 private final IdentityService identities;private final AcademicService academic;
 public IdentityController(IdentityService identities,AcademicService academic){this.identities=identities;this.academic=academic;}
 public record ContactSettings(boolean muted,boolean blocked){}
 public record ReportInput(String reason){}
 @GetMapping("/profile") public Map<String,String> profile(Principal p){return identities.profile(p.getName());}
 @GetMapping("/preferences") public IdentityService.Preferences preferences(Principal p){return identities.preferences(p.getName());}
 @PutMapping("/preferences") public IdentityService.Preferences preferences(Principal p,@RequestBody IdentityService.Preferences input){return identities.preferences(p.getName(),input);}
 @GetMapping("/contacts") public List<IdentityService.Contact> contacts(Principal p){return identities.contacts(p.getName());}
 @PutMapping("/contacts/{username}/preferences") public Map<String,Boolean> contact(Principal p,@PathVariable String username,@RequestBody ContactSettings input){
  identities.contactPreference(p.getName(),username,input.muted(),input.blocked());return Map.of("saved",true);
 }
 @PostMapping("/contacts/{username}/reports") public Map<String,String> report(Principal p,@PathVariable String username,@RequestBody ReportInput input){
  return Map.of("id",identities.report(p.getName(),username,input.reason()));
 }
 @GetMapping("/agenda") public AcademicService.Agenda agenda(Principal p){return academic.agenda(p.getName());}
}
