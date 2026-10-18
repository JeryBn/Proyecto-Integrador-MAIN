package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/v1/moderation")
public class ModerationController{
 private final CommunicationService service;
 public ModerationController(CommunicationService service){this.service=service;}
 @GetMapping("/messages") public List<CommunicationService.Message> queue(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.queue(p.getName(),offset,limit);}
 @PostMapping("/messages/{id}/review") public Map<String,Boolean> review(Principal p,@PathVariable String id,@RequestBody CommunicationService.Review input){
  service.review(p.getName(),id,input);return Map.of("reviewed",true);
 }
}
