package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/reminders/preferences")
public class ReminderController {
 private final ReminderService service;
 public ReminderController(ReminderService service){this.service=service;}
 @GetMapping public ReminderService.Settings get(Principal p){return service.settings(p.getName());}
 @PutMapping public ReminderService.Settings put(Principal p,@RequestBody ReminderService.Settings value){return service.settings(p.getName(),value);}
}
