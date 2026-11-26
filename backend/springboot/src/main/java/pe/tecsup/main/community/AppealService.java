package pe.tecsup.main.community;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class AppealService {
 private final JdbcTemplate db; private final IdentityService users; private final Clock clock; private final AdministrativeAudit audit;
 public AppealService(JdbcTemplate db,IdentityService users,Clock clock,AdministrativeAudit audit){this.db=db;this.users=users;this.clock=clock;this.audit=audit;}
 public record Input(String kind,String subjectId,int revision,String contentHash,String reason){}
 public record Appeal(String id,String kind,String subjectId,int revision,String state,String reason,String createdAt,String decisionReason,String decidedAt){}
 private Appeal row(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new Appeal(r.getString("id"),r.getString("subject_kind"),r.getString("subject_id"),r.getInt("revision"),r.getString("state"),r.getString("request_reason"),r.getTimestamp("created_at").toLocalDateTime().toString(),Objects.toString(r.getString("decision_reason"),""),r.getTimestamp("decided_at")==null?"":r.getTimestamp("decided_at").toLocalDateTime().toString());}
 public List<Appeal> list(String actor,int offset,int limit){users.active(actor);IdentityService.page(offset,limit);return db.query("SELECT * FROM moderation_appeal WHERE owner=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",this::row,actor,limit,offset);}
 private DomainException changed(){return new DomainException(HttpStatus.CONFLICT,"APPEAL_CONFLICT","El contenido cambió, ya fue eliminado o no admite una apelación. Actualiza la pantalla.");}
 @Transactional public Appeal create(String actor,Input input){
  if(users.active(actor).role().equals("ADMIN"))throw DomainException.denied();
  String kind=input.kind(),id=input.subjectId(),reason=IdentityService.text(input.reason(),1000,"Motivo de apelación");
  if(!Set.of("messages","statuses").contains(Objects.toString(kind,""))||id==null||id.length()>36||input.contentHash()==null||!input.contentHash().matches("[a-f0-9]{64}"))throw DomainException.invalid("Contenido y versión requeridos.");
  Map<String,Object> content;
  if(kind.equals("messages")){
   var messages=db.queryForList("SELECT conversation_id,sender FROM message WHERE id=?",id);
   if(messages.isEmpty()||!actor.equals(messages.getFirst().get("sender")))throw DomainException.missing();
   db.queryForList("SELECT id FROM conversation WHERE id=? FOR UPDATE",messages.getFirst().get("conversation_id"));
   // Current locking reads are required under MySQL REPEATABLE READ: the account
   // lookup can establish an older snapshot before we acquire the parent lock.
   var revisions=db.queryForList("SELECT * FROM message_revision WHERE message_id=? ORDER BY revision DESC LIMIT 1 FOR UPDATE",id);
   if(revisions.isEmpty())throw DomainException.missing();content=revisions.getFirst();
   if(((Number)content.get("revision")).intValue()!=input.revision())throw changed();
  }else{
   var statuses=db.queryForList("SELECT * FROM status_update WHERE id=? FOR UPDATE",id);
   if(statuses.isEmpty()||!actor.equals(statuses.getFirst().get("owner")))throw DomainException.missing();
   content=statuses.getFirst();if(input.revision()!=0)throw changed();
  }
  if(!input.contentHash().equals(content.get("content_hash"))||content.get("purged_at")!=null)throw changed();
  var previous=db.query("SELECT * FROM moderation_appeal WHERE owner=? AND subject_kind=? AND subject_id=? AND revision=? AND content_hash=? FOR UPDATE",this::row,actor,kind,id,input.revision(),input.contentHash());
  if(!previous.isEmpty())return previous.getFirst(); // Retry cannot replace the original reason or decision.
  if(!"REJECTED".equals(content.get("state")))throw changed();
  String appealId=UUID.randomUUID().toString();
  db.update("INSERT INTO moderation_appeal(id,owner,subject_kind,subject_id,revision,content_hash,request_reason,state,created_at) VALUES(?,?,?,?,?,?,?,'OPEN',?)",appealId,actor,kind,id,input.revision(),input.contentHash(),reason,Timestamp.valueOf(LocalDateTime.now(clock)));
  audit.record(actor,"APPEAL_OPENED",id); // No reason or content in audit/push.
  return db.query("SELECT * FROM moderation_appeal WHERE id=?",this::row,appealId).getFirst();
 }
}
