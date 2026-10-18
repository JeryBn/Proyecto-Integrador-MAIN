package pe.tecsup.main.community;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class StatusService {
 private final JdbcTemplate db;private final IdentityService users;private final Clock clock;
 public StatusService(JdbcTemplate db,IdentityService users,Clock clock){this.db=db;this.users=users;this.clock=clock;}
 public record Update(String id,String owner,String name,String text,String background,String audience,String kind,String state,String contentHash,String createdAt,String expiresAt,String reason,boolean seen,int views){}
 public record Review(String contentHash,boolean approved,String reason){}
 private LocalDateTime now(){return LocalDateTime.now(clock);}
 private Update row(String id){return db.query("SELECT s.*,u.display_name FROM status_update s JOIN main_user u ON s.owner=u.username WHERE s.id=?",(r,n)->new Update(r.getString("id"),r.getString("owner"),r.getString("display_name"),r.getString("body"),r.getString("background"),r.getString("audience"),r.getString("kind"),r.getString("state"),r.getString("content_hash"),r.getTimestamp("created_at").toLocalDateTime().atZone(clock.getZone()).toInstant().toString(),r.getTimestamp("expires_at").toLocalDateTime().atZone(clock.getZone()).toInstant().toString(),Objects.toString(r.getString("reason"),""),false,0),id).stream().findFirst().orElseThrow(DomainException::missing);}
 private boolean visible(String actor,Update s){
  if(s.state().equals("DELETED")||!Instant.parse(s.expiresAt()).isAfter(clock.instant()))return false;
  if(s.owner().equals(actor))return true;
  return users.account(s.owner()).enabled() && s.state().equals("APPROVED") && s.audience().equals("CONTACTS") && users.preferences(s.owner()).statusAudience().equals("CONTACTS") && users.contacts(actor,s.owner()) && !users.blocked(actor,s.owner()) && db.queryForObject("SELECT COUNT(*) FROM status_recipient WHERE status_id=? AND username=?",Long.class,s.id(),actor)>0;
 }
 public Update get(String actor,String id){users.active(actor);var s=row(id);if(!visible(actor,s))throw DomainException.missing();return decorate(actor,s);}
 private Update decorate(String actor,Update s){boolean seen=db.queryForObject("SELECT COUNT(*) FROM status_view WHERE status_id=? AND username=?",Long.class,s.id(),actor)>0;int views=s.owner().equals(actor)?db.queryForObject("SELECT COUNT(*) FROM status_view WHERE status_id=?",Integer.class,s.id()):0;return new Update(s.id(),s.owner(),s.name(),s.text(),s.background(),s.audience(),s.kind(),s.state(),s.contentHash(),s.createdAt(),s.expiresAt(),s.reason(),seen,views);}
 public List<Update> feed(String actor){users.active(actor);return db.queryForList("SELECT id FROM status_update WHERE expires_at>? AND state<>'DELETED' AND (owner=? OR id IN (SELECT status_id FROM status_recipient WHERE username=?)) ORDER BY created_at DESC,id LIMIT 200",String.class,Timestamp.valueOf(now()),actor,actor).stream().map(this::row).filter(s->visible(actor,s)).map(s->decorate(actor,s)).toList();}
 @Transactional public Update create(String actor,String key,String text,String background,String audience,StatusMedia.Validated media){
  var user=users.active(actor);if(!Set.of("ALUMNO","DOCENTE").contains(user.role()))throw DomainException.denied();
  db.queryForObject("SELECT username FROM main_user WHERE username=? FOR UPDATE",String.class,actor);
  try{UUID.fromString(key);}catch(Exception e){throw DomainException.invalid("Identificador de envío inválido.");}
  if(!Set.of("CONTACTS","PRIVATE").contains(Objects.toString(audience,"")))throw DomainException.invalid("Audiencia inválida.");
  if(!Objects.toString(background,"").matches("#[0-9a-fA-F]{6}"))throw DomainException.invalid("Color inválido.");
  String body=Objects.toString(text,"").strip();if(body.length()>700||(body.isBlank()&&media==null))throw DomainException.invalid("Escribe un estado de hasta 700 caracteres o añade multimedia.");
  byte[] bytes=media==null?new byte[0]:media.bytes();String kind=media==null?"TEXT":media.kind();String hash;
  try{var digest=MessageDigest.getInstance("SHA-256");digest.update((kind+"\n"+body+"\n"+background+"\n"+audience+"\n").getBytes(StandardCharsets.UTF_8));hash=HexFormat.of().formatHex(digest.digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}
  var old=db.queryForList("SELECT id FROM status_update WHERE owner=? AND client_key=?",String.class,actor,key);
  if(!old.isEmpty()){var existing=row(old.getFirst());if(existing.state().equals("DELETED")||!Instant.parse(existing.expiresAt()).isAfter(clock.instant()))throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","El estado caducó o fue eliminado. Crea una actualización nueva.");if(!existing.contentHash().equals(hash))throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","El envío ya existe con otro contenido.");return decorate(actor,existing);}
  if(db.queryForObject("SELECT COUNT(*) FROM status_update WHERE owner=? AND created_at>?",Long.class,actor,Timestamp.valueOf(now().minusHours(24)))>=30)throw DomainException.invalid("Máximo 30 estados por 24 horas.");
  String id=UUID.randomUUID().toString();var time=now();
  db.update("INSERT INTO status_update(id,owner,client_key,body,background,audience,kind,state,content_hash,created_at,expires_at) VALUES(?,?,?,?,?,?,?,'PENDING',?,?,?)",id,actor,key,body,background,audience,kind,hash,Timestamp.valueOf(time),Timestamp.valueOf(time.plusHours(24)));
  for(int offset=0,sequence=0;offset<bytes.length;offset+=60000,sequence++)db.update("INSERT INTO status_media_chunk VALUES(?,?,?)",id,sequence,Arrays.copyOfRange(bytes,offset,Math.min(offset+60000,bytes.length)));
  if(audience.equals("CONTACTS"))for(var contact:users.contacts(actor))if(!users.blocked(actor,contact.username()))db.update("INSERT INTO status_recipient VALUES(?,?)",id,contact.username());
  return row(id);
 }
 public String reviewKind(String actor,String id){users.admin(actor);var s=row(id);if(!s.state().equals("PENDING")||!Instant.parse(s.expiresAt()).isAfter(clock.instant()))throw DomainException.missing();return s.kind();}
 public void canPublish(String actor){if(!Set.of("ALUMNO","DOCENTE").contains(users.active(actor).role()))throw DomainException.denied();}
 public byte[] media(String actor,String id,boolean admin){if(admin){users.admin(actor);var s=row(id);if(!s.state().equals("PENDING")||!Instant.parse(s.expiresAt()).isAfter(clock.instant()))throw DomainException.missing();}else get(actor,id);var output=new ByteArrayOutputStream();db.query("SELECT payload FROM status_media_chunk WHERE status_id=? ORDER BY sequence_no",(org.springframework.jdbc.core.RowCallbackHandler)r->output.writeBytes(r.getBytes(1)),id);return output.toByteArray();}
 @Transactional public void seen(String actor,String id){db.queryForList("SELECT id FROM status_update WHERE id=? FOR UPDATE",String.class,id);var s=get(actor,id);if(s.owner().equals(actor))return;if(db.queryForObject("SELECT COUNT(*) FROM status_view WHERE status_id=? AND username=?",Long.class,id,actor)==0)db.update("INSERT INTO status_view VALUES(?,?,?)",id,actor,Timestamp.valueOf(now()));}
 @Transactional public void delete(String actor,String id){users.active(actor);var s=row(id);if(!s.owner().equals(actor))throw DomainException.missing();db.update("UPDATE status_update SET state='DELETED',body='',reason=NULL WHERE id=?",id);db.update("DELETE FROM status_media_chunk WHERE status_id=?",id);}
 public List<Update> queue(String actor,int offset,int limit){users.admin(actor);IdentityService.page(offset,limit);return db.queryForList("SELECT id FROM status_update WHERE state='PENDING' AND expires_at>? ORDER BY created_at,id LIMIT ? OFFSET ?",String.class,Timestamp.valueOf(now()),limit,offset).stream().map(this::row).toList();}
 @Transactional public void review(String actor,String id,Review review){users.admin(actor);String reason=review.approved()?"":IdentityService.text(review.reason(),300,"motivo");int changed=db.update("UPDATE status_update SET state=?,reason=?,reviewed_by=?,reviewed_at=? WHERE id=? AND state='PENDING' AND content_hash=? AND expires_at>?",review.approved()?"APPROVED":"REJECTED",reason,actor,Timestamp.valueOf(now()),id,review.contentHash(),Timestamp.valueOf(now()));if(changed!=1)throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","El estado cambió o caducó. Actualiza la cola.");}
}
