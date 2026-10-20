package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class CommunicationController{
 private final CommunicationService service;
 public CommunicationController(CommunicationService service){this.service=service;}
 public record Direct(String username){}
 public record Group(String title){}
 public record Invite(String username){}
 public record Invitation(boolean accept){}
 public record Membership(boolean administrator,boolean remove){}
 public record Cancel(int expectedRevision){}
 @GetMapping("/conversations") public List<CommunicationService.Conversation> list(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.conversations(p.getName(),offset,limit);}
 @PostMapping("/conversations/direct") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,String> direct(Principal p,@RequestBody Direct input){return Map.of("id",service.direct(p.getName(),input.username()));}
 @PostMapping("/conversations/groups") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,String> group(Principal p,@RequestBody Group input){return Map.of("id",service.group(p.getName(),input.title()));}
 @PostMapping("/conversations/{id}/invitations") public Map<String,Boolean> invite(Principal p,@PathVariable String id,@RequestBody Invite input){service.invite(p.getName(),id,input.username());return Map.of("invited",true);}
 @PostMapping("/conversations/{id}/invitation") public Map<String,Boolean> invitation(Principal p,@PathVariable String id,@RequestBody Invitation input){service.invitation(p.getName(),id,input.accept());return Map.of("saved",true);}
 @GetMapping("/conversations/{id}/members") public List<CommunicationService.Member> members(Principal p,@PathVariable String id){return service.members(p.getName(),id);}
 @PutMapping("/conversations/{id}/members/{username}") public Map<String,Boolean> member(Principal p,@PathVariable String id,@PathVariable String username,@RequestBody Membership input){
  service.membership(p.getName(),id,username,input.administrator(),input.remove());return Map.of("saved",true);
 }
 @GetMapping("/conversations/{id}/messages") public List<CommunicationService.Message> messages(Principal p,@PathVariable String id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.messages(p.getName(),id,offset,limit);}
 @PostMapping(value="/conversations/{id}/messages",consumes="application/json") @ResponseStatus(HttpStatus.ACCEPTED)
 public CommunicationService.Message send(Principal p,@PathVariable String id,@RequestBody CommunicationService.Send input){return service.send(p.getName(),id,input);}
 @PutMapping("/messages/{id}") public CommunicationService.Message edit(Principal p,@PathVariable String id,@RequestBody CommunicationService.Edit input){return service.edit(p.getName(),id,input);}
 @PostMapping("/messages/{id}/cancel") public Map<String,Boolean> cancel(Principal p,@PathVariable String id,@RequestBody Cancel input){service.cancel(p.getName(),id,input.expectedRevision());return Map.of("cancelled",true);}
}
