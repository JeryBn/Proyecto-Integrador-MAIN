package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController public class StatusController {
 private final StatusService service;private final StatusMedia media;
 public StatusController(StatusService service,StatusMedia media){this.service=service;this.media=media;}
 @GetMapping("/api/v1/statuses") public List<StatusService.Update> feed(Principal p){return service.feed(p.getName());}
 @PostMapping(value="/api/v1/statuses",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public StatusService.Update create(Principal p,@RequestParam String clientKey,@RequestParam(defaultValue="")String text,@RequestParam String background,@RequestParam String audience,@RequestPart(required=false)MultipartFile file){service.canPublish(p.getName());return service.create(p.getName(),clientKey,text,background,audience,media.validate(file));}
 @GetMapping("/api/v1/statuses/{id}/media") public ResponseEntity<byte[]> media(Principal p,@PathVariable String id){var status=service.get(p.getName(),id);return response(service.media(p.getName(),id,false),status.kind());}
 @PostMapping("/api/v1/statuses/{id}/seen") public Map<String,Boolean> seen(Principal p,@PathVariable String id){service.seen(p.getName(),id);return Map.of("seen",true);}
 @PostMapping("/api/v1/statuses/{id}/delete") public Map<String,Boolean> delete(Principal p,@PathVariable String id){service.delete(p.getName(),id);return Map.of("deleted",true);}
 @GetMapping("/api/admin/v1/moderation/statuses") public List<StatusService.Update> queue(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.queue(p.getName(),offset,limit);}
 @GetMapping("/api/admin/v1/moderation/statuses/{id}/media") public ResponseEntity<byte[]> reviewMedia(Principal p,@PathVariable String id){String kind=service.reviewKind(p.getName(),id);return response(service.media(p.getName(),id,true),kind);}
 @PostMapping("/api/admin/v1/moderation/statuses/{id}/review") public Map<String,Boolean> review(Principal p,@PathVariable String id,@RequestBody StatusService.Review review){service.review(p.getName(),id,review);return Map.of("reviewed",true);}
 private ResponseEntity<byte[]> response(byte[] bytes,String kind){String type=switch(kind){case "IMAGE"->"image/jpeg";case "VIDEO"->"video/mp4";case "AUDIO"->"audio/mp4";default->"application/octet-stream";};return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(bytes);}
}
