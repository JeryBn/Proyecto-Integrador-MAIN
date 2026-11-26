package pe.tecsup.main.community;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MessageAttachmentController {
 private final CommunicationService messages;private final MessageAttachments attachments;
 public MessageAttachmentController(CommunicationService messages,MessageAttachments attachments){this.messages=messages;this.attachments=attachments;}
 @PostMapping(value="/api/v1/conversations/{id}/messages",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @ResponseStatus(HttpStatus.ACCEPTED)
 public CommunicationService.Message send(Principal actor,@PathVariable String id,@RequestParam String clientKey,@RequestParam(defaultValue="")String text,@RequestPart MultipartFile file){
  messages.authorizeAttachment(actor.getName(),id);
  return messages.sendMedia(actor.getName(),id,new CommunicationService.Send(clientKey,text),attachments.validate(file));
 }
 @GetMapping("/api/v1/messages/{id}/attachment")
 public ResponseEntity<byte[]> download(Principal actor,@PathVariable String id,@RequestParam int revision){return response(messages.attachment(actor.getName(),id,revision,false));}
 @GetMapping("/api/admin/v1/moderation/messages/{id}/attachment")
 public ResponseEntity<byte[]> review(Principal actor,@PathVariable String id,@RequestParam int revision){return response(messages.attachment(actor.getName(),id,revision,true));}
 private ResponseEntity<byte[]> response(CommunicationService.MediaDownload media){
  var disposition=media.kind().equals("FILE")?ContentDisposition.attachment():ContentDisposition.inline();
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.mime())).cacheControl(CacheControl.noStore())
   .header("X-Content-Type-Options","nosniff").header("Content-Disposition",disposition.filename(media.name()).build().toString()).body(media.bytes());
 }
}
