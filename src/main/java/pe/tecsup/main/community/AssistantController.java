package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/assistant")
public class AssistantController {
 private final AssistantService service;
 public AssistantController(AssistantService service){this.service=service;}
 @PostMapping public AssistantService.Reply ask(Principal actor,@RequestBody AssistantService.Input input){return service.ask(actor.getName(),input);}
}
