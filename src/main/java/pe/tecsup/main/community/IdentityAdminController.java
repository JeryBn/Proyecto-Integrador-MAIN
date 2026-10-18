package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/admin/v1")
public class IdentityAdminController {
 private final IdentityService identities;private final AcademicService academic;
 public IdentityAdminController(IdentityService identities,AcademicService academic){this.identities=identities;this.academic=academic;}
 public record Guardian(String guardian,long studentId){}
 public record Contact(String first,String second){}
 public record Password(String password){@Override public String toString(){return "Password[redacted]";}}
 @GetMapping("/users") public List<IdentityService.Account> users(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return identities.users(p.getName(),offset,limit);}
 @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
 public IdentityService.Account create(Principal p,@RequestBody IdentityService.NewAccount input){return identities.create(p.getName(),input);}
 @PostMapping("/guardians") public Map<String,Boolean> guardian(Principal p,@RequestBody Guardian input){identities.verifyGuardian(p.getName(),input.guardian(),input.studentId());return Map.of("verified",true);}
 @DeleteMapping("/guardians/{guardian}/{studentId}") public void guardian(Principal p,@PathVariable String guardian,@PathVariable long studentId){identities.revokeGuardian(p.getName(),guardian,studentId);}
 @PostMapping("/contacts") public Map<String,Boolean> contact(Principal p,@RequestBody Contact input){identities.approveContact(p.getName(),input.first(),input.second());return Map.of("approved",true);}
 @PostMapping("/users/{username}/reset-password") public void reset(Principal p,@PathVariable String username,@RequestBody Password input){identities.resetPassword(p.getName(),username,input.password());}
 @PostMapping("/users/{username}/disable") public void disable(Principal p,@PathVariable String username){identities.disable(p.getName(),username);}
 @GetMapping("/reports") public List<Map<String,Object>> reports(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return identities.reports(p.getName(),offset,limit);}
 @PostMapping("/timetable") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,String> slot(Principal p,@RequestBody AcademicService.NewSlot input){return Map.of("id",academic.createSlot(p.getName(),input));}
 @DeleteMapping("/timetable/{id}") public void slot(Principal p,@PathVariable String id){academic.removeSlot(p.getName(),id);}
}
