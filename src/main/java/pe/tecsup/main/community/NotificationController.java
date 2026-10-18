package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
 private final NotificationService service;
 public NotificationController(NotificationService service){this.service=service;}
 @GetMapping public List<NotificationService.Notice> list(Principal p,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="30") int limit){return service.list(p.getName(),offset,limit);}
 @PostMapping("/{id}/read") public Map<String,Boolean> read(Principal p,@PathVariable String id){service.read(p.getName(),id);return Map.of("read",true);}
}
