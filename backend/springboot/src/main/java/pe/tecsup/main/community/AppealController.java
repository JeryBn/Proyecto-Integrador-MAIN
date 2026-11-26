package pe.tecsup.main.community;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/appeals")
public class AppealController {
 private final AppealService service;
 public AppealController(AppealService service){this.service=service;}
 @GetMapping public List<AppealService.Appeal> list(Principal actor,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.list(actor.getName(),offset,limit);}
 @PostMapping public AppealService.Appeal create(Principal actor,@RequestBody AppealService.Input input){return service.create(actor.getName(),input);}
}
