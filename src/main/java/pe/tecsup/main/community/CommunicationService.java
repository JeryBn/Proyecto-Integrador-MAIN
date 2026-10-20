package pe.tecsup.main.community;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@Service
public class CommunicationService {
 private final JdbcTemplate db;private final IdentityService users;private final Clock clock;private final NotificationService notifications;private final AdministrativeAudit audit;
 public CommunicationService(JdbcTemplate db,IdentityService users,Clock clock,NotificationService notifications,AdministrativeAudit audit){this.db=db;this.users=users;this.clock=clock;this.notifications=notifications;this.audit=audit;}
 public record Conversation(String id,String kind,String title,boolean administrator,String state){}
 public record Member(String username,String name,String role,String state,boolean administrator){}
 public record Message(String id,String conversationId,String sender,String senderName,int revision,String text,String state,String reason,
                       boolean correctedBeforeDelivery,boolean editedAfterDelivery,String createdAt,String contentHash){}
 public record Send(String clientKey,String text){}
 public record Edit(int expectedRevision,String text){}
 public record Review(int revision,String contentHash,boolean approved,String reason){}
 private LocalDateTime now(){return LocalDateTime.now(clock);}
 private void lock(String id){if(db.queryForList("SELECT id FROM conversation WHERE id=? FOR UPDATE",String.class,id).isEmpty())throw DomainException.missing();}
 private void member(String actor,String id){
  users.active(actor);
  if(db.queryForObject("SELECT COUNT(*) FROM conversation_member WHERE conversation_id=? AND username=? AND state='ACTIVE'",Long.class,id,actor)==0)throw DomainException.missing();
 }
 private void administrator(String actor,String id){
  member(actor,id);
  if(db.queryForObject("SELECT COUNT(*) FROM conversation_member WHERE conversation_id=? AND username=? AND administrator=TRUE",Long.class,id,actor)==0)throw DomainException.denied();
 }
 private boolean direct(String id){return db.queryForObject("SELECT kind FROM conversation WHERE id=?",String.class,id).equals("DIRECT");}
 private void canSend(String actor,String id){
  member(actor,id);
  if(direct(id)){
   var others=db.queryForList("SELECT username FROM conversation_member WHERE conversation_id=? AND username<>?",String.class,id,actor);
   if(others.isEmpty()||!users.contacts(actor,others.getFirst())||users.blocked(actor,others.getFirst()))throw DomainException.denied();
   users.active(others.getFirst());
  }
 }
 public List<Conversation> conversations(String actor,int offset,int limit){
  users.active(actor);IdentityService.page(offset,limit);
  return db.query("""
   SELECT c.id,c.kind,CASE WHEN c.kind='DIRECT' THEN
    (SELECT u.display_name FROM conversation_member other JOIN main_user u ON u.username=other.username WHERE other.conversation_id=c.id AND other.username<>m.username)
    ELSE c.title END AS title,m.administrator,m.state FROM conversation c JOIN conversation_member m ON m.conversation_id=c.id
   WHERE m.username=? AND m.state IN ('ACTIVE','INVITED') ORDER BY c.created_at DESC,c.id LIMIT ? OFFSET ?
   """,(r,n)->new Conversation(r.getString(1),r.getString(2),r.getString(3),r.getBoolean(4),r.getString(5)),actor,limit,offset);
 }
 @Transactional public String direct(String actor,String target){
  users.active(actor);users.active(target);
  if(actor.equals(target)||!users.contacts(actor,target)||users.blocked(actor,target))throw DomainException.denied();
  String key=actor.compareTo(target)<0?actor+"|"+target:target+"|"+actor;
  db.queryForObject("SELECT username FROM main_user WHERE username=? FOR UPDATE",String.class,actor.compareTo(target)<0?actor:target);
  var existing=db.queryForList("SELECT id FROM conversation WHERE direct_key=?",String.class,key);if(!existing.isEmpty())return existing.getFirst();
  String id=UUID.randomUUID().toString();var date=Timestamp.valueOf(now());
  db.update("INSERT INTO conversation VALUES(?,?,?,?,?,?)",id,"DIRECT","Conversación privada",actor,date,key);
  for(String user:List.of(actor,target))db.update("INSERT INTO conversation_member VALUES(?,?,'ACTIVE',FALSE,?)",id,user,date);
  return id;
 }
 @Transactional public String group(String actor,String title){
  if(users.active(actor).role().equals("ADMIN"))throw DomainException.denied();
  String id=UUID.randomUUID().toString();var date=Timestamp.valueOf(now());
  db.update("INSERT INTO conversation VALUES(?,?,?,?,?,NULL)",id,"GROUP",IdentityService.text(title,100,"nombre del grupo"),actor,date);
  db.update("INSERT INTO conversation_member VALUES(?,?,'ACTIVE',TRUE,?)",id,actor,date);audit.record(actor,"GROUP_CREATED",id);return id;
 }
 @Transactional public void invite(String actor,String id,String target){
  lock(id);administrator(actor,id);users.active(target);
  if(direct(id)||!users.contacts(actor,target)||users.blocked(actor,target))throw DomainException.denied();
  var states=db.queryForList("SELECT state FROM conversation_member WHERE conversation_id=? AND username=?",String.class,id,target);
  if(states.isEmpty())db.update("INSERT INTO conversation_member VALUES(?,?,'INVITED',FALSE,NULL)",id,target);
  else if(!states.getFirst().equals("ACTIVE"))db.update("UPDATE conversation_member SET state='INVITED',administrator=FALSE,joined_at=NULL WHERE conversation_id=? AND username=?",id,target);
  if(states.isEmpty()||!states.getFirst().equals("ACTIVE")){notifications.enqueue(target,actor,id,null,"INVITATION","invitation:"+id+":"+target);audit.record(actor,"GROUP_INVITED",id+":"+target);}
 }
 @Transactional public void invitation(String actor,String id,boolean accept){
  lock(id);users.active(actor);
  if(db.update("UPDATE conversation_member SET state=?,joined_at=? WHERE conversation_id=? AND username=? AND state='INVITED'",
   accept?"ACTIVE":"REMOVED",accept?Timestamp.valueOf(now()):null,id,actor)==0)throw DomainException.missing();
  audit.record(actor,accept?"GROUP_INVITATION_ACCEPTED":"GROUP_INVITATION_REJECTED",id);
 }
 public List<Member> members(String actor,String id){
  member(actor,id);return db.query("SELECT m.username,u.display_name,u.role,m.state,m.administrator FROM conversation_member m JOIN main_user u ON u.username=m.username WHERE m.conversation_id=? AND m.state<>'REMOVED' ORDER BY m.username",
   (r,n)->new Member(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)),id);
 }
 @Transactional public void membership(String actor,String id,String target,boolean administrator,boolean remove){
  lock(id);member(actor,id);if(direct(id))throw DomainException.denied();
  if(!actor.equals(target)||!remove)administrator(actor,id);
  var rows=db.queryForList("SELECT administrator FROM conversation_member WHERE conversation_id=? AND username=? AND state='ACTIVE'",Boolean.class,id,target);
  if(rows.isEmpty())throw DomainException.missing();
  if(rows.getFirst()&&(remove||!administrator)&&db.queryForObject("SELECT COUNT(*) FROM conversation_member WHERE conversation_id=? AND administrator=TRUE AND state='ACTIVE'",Long.class,id)<2)
   throw DomainException.invalid("Designa otro administrador antes de salir o retirar al último administrador.");
  db.update("UPDATE conversation_member SET administrator=?,state=? WHERE conversation_id=? AND username=?",administrator&&!remove,remove?"REMOVED":"ACTIVE",id,target);
  audit.record(actor,remove?"GROUP_MEMBER_REMOVED":"GROUP_MEMBER_ROLE_CHANGED",id+":"+target);
 }
 private String hash(String text){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
  catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private int latest(String id){return db.queryForObject("SELECT MAX(revision) FROM message_revision WHERE message_id=?",Integer.class,id);}
 private void revision(String id,int revision,String text){
  db.update("INSERT INTO message_revision(message_id,revision,body,state,content_hash) VALUES(?,?,?,'PENDING',?)",id,revision,text,hash(text));
 }
 @Transactional public Message send(String actor,String conversation,Send input){
  lock(conversation);canSend(actor,conversation);String text=IdentityService.text(input.text(),4000,"mensaje");
  try{UUID.fromString(input.clientKey());}catch(Exception e){throw DomainException.invalid("clientKey debe ser UUID.");}
  var prior=db.queryForList("SELECT id FROM message WHERE sender=? AND client_key=?",String.class,actor,input.clientKey());
  if(!prior.isEmpty()){
   var own=message(actor,prior.getFirst());
   String original=db.queryForObject("SELECT body FROM message_revision WHERE message_id=? AND revision=1",String.class,own.id());
   if(!own.conversationId().equals(conversation)||!original.equals(text))throw new DomainException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","La clave de envío ya tiene otro contenido.");
   return own;
  }
  String id=UUID.randomUUID().toString();
  db.update("INSERT INTO message(id,conversation_id,sender,client_key,created_at) VALUES(?,?,?,?,?)",id,conversation,actor,input.clientKey(),Timestamp.valueOf(now()));
  revision(id,1,text);return message(actor,id);
 }
 @Transactional public Message edit(String actor,String id,Edit input){
  var header=header(id);String conversation=(String)header.get("conversation_id");lock(conversation);canSend(actor,conversation);
  if(!actor.equals(header.get("sender")))throw DomainException.missing();
  int current=latest(id);if(input.expectedRevision()!=current)throw stale();
  revision(id,current+1,IdentityService.text(input.text(),4000,"mensaje"));return message(actor,id);
 }
 private DomainException stale(){return new DomainException(HttpStatus.CONFLICT,"REVISION_CONFLICT","La versión cambió. Actualiza y revisa el contenido actual.");}
 private Map<String,Object> header(String id){
  var rows=db.queryForList("SELECT id,conversation_id,sender,created_at,published_revision,corrected_before,edited_after FROM message WHERE id=?",id);
  if(rows.isEmpty())throw DomainException.missing();
  var result=new HashMap<String,Object>();rows.getFirst().forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;
 }
 public Message message(String actor,String id){
  var h=header(id);String conversation=(String)h.get("conversation_id"),sender=(String)h.get("sender");member(actor,conversation);
  users.active(sender);
  if(!sender.equals(actor)&&users.blocked(actor,sender))throw DomainException.missing();
  var joined=db.queryForObject("SELECT joined_at FROM conversation_member WHERE conversation_id=? AND username=?",Timestamp.class,conversation,actor);
  if(!sender.equals(actor)&&((Timestamp)h.get("created_at")).before(joined))throw DomainException.missing();
  Integer revision=sender.equals(actor)?latest(id):(h.get("published_revision")==null?null:((Number)h.get("published_revision")).intValue());
  if(revision==null)throw DomainException.missing();
  return dto(h,revision,sender.equals(actor),false);
 }
 private Message dto(Map<String,Object> h,int revision,boolean own,boolean review){
  String id=(String)h.get("id"),sender=(String)h.get("sender");
  return db.queryForObject("SELECT * FROM message_revision WHERE message_id=? AND revision=?",(r,n)->new Message(id,(String)h.get("conversation_id"),sender,
   users.account(sender).name(),revision,r.getString("body"),r.getString("state"),own||review?Objects.toString(r.getString("reason"),""):"",
   Boolean.TRUE.equals(h.get("corrected_before")),Boolean.TRUE.equals(h.get("edited_after")),h.get("created_at").toString(),own||review?r.getString("content_hash"):""),id,revision);
 }
 public List<Message> messages(String actor,String id,int offset,int limit){
  member(actor,id);IdentityService.page(offset,limit);
  // Filter in SQL before paging so hidden drafts cannot shift a recipient's visible pages.
  var ids=db.queryForList("""
   SELECT m.id FROM message m JOIN conversation_member cm ON cm.conversation_id=m.conversation_id AND cm.username=?
   JOIN main_user sender ON sender.username=m.sender
   WHERE m.conversation_id=? AND sender.enabled=TRUE AND
   (m.sender=? OR (m.published_revision IS NOT NULL AND m.created_at>=cm.joined_at AND NOT EXISTS(
    SELECT 1 FROM contact_preference p WHERE p.blocked=TRUE AND ((p.owner=? AND p.subject=m.sender) OR (p.owner=m.sender AND p.subject=?)))))
   ORDER BY m.created_at DESC,m.id DESC LIMIT ? OFFSET ?
   """,String.class,actor,id,actor,actor,actor,limit,offset);
  Collections.reverse(ids);return ids.stream().map(key->message(actor,key)).toList();
 }
 public List<Message> queue(String actor,int offset,int limit){
  users.admin(actor);IdentityService.page(offset,limit);
  return db.queryForList("""
   SELECT m.id FROM message m JOIN message_revision r ON r.message_id=m.id
   WHERE r.state='PENDING' AND r.revision=(SELECT MAX(r2.revision) FROM message_revision r2 WHERE r2.message_id=m.id)
   ORDER BY m.created_at,m.id LIMIT ? OFFSET ?
   """,String.class,limit,offset).stream().map(id->dto(header(id),latest(id),false,true)).toList();
 }
 @Transactional public void review(String actor,String id,Review input){
  users.admin(actor);var h=header(id);String conversation=(String)h.get("conversation_id");lock(conversation);
  if(actor.equals(h.get("sender")))throw DomainException.denied();
  int current=latest(id);var draft=dto(header(id),current,true,true);
  if(input.revision()!=current||!Objects.equals(input.contentHash(),draft.contentHash())||!draft.state().equals("PENDING"))throw stale();
  // Approval is human and explicit; no absent/uncertain AI result can publish.
  canSend((String)h.get("sender"),conversation);
  String reason=input.approved()?"":IdentityService.text(input.reason(),300,"categoría de rechazo");
  db.update("UPDATE message_revision SET state=?,reason=?,reviewed_by=?,reviewed_at=? WHERE message_id=? AND revision=?",
   input.approved()?"APPROVED":"REJECTED",reason,actor,Timestamp.valueOf(now()),id,current);
  audit.record(actor,input.approved()?"MESSAGE_APPROVED":"MESSAGE_REJECTED",id+":"+current);
  if(input.approved()){
   boolean published=h.get("published_revision")!=null;
   boolean corrected=!published&&db.queryForObject("SELECT COUNT(*) FROM message_revision WHERE message_id=? AND state='REJECTED'",Long.class,id)>0;
   db.update("UPDATE message SET published_revision=?,corrected_before=?,edited_after=? WHERE id=?",current,
    published?Boolean.TRUE.equals(h.get("corrected_before")):corrected,published,id);
   if(!published) for(String recipient:db.queryForList("SELECT username FROM conversation_member WHERE conversation_id=? AND state='ACTIVE' AND joined_at<=?",String.class,conversation,h.get("created_at")))
    notifications.enqueue(recipient,(String)h.get("sender"),conversation,id,"MESSAGE","message:"+id);
  }
 }
 @Transactional public void cancel(String actor,String id,int expected){
  var h=header(id);String conversation=(String)h.get("conversation_id");lock(conversation);member(actor,conversation);
  if(!actor.equals(h.get("sender")))throw DomainException.missing();
  if(latest(id)!=expected)throw stale();
  if(db.update("UPDATE message_revision SET state='CANCELLED' WHERE message_id=? AND revision=? AND state IN ('PENDING','REJECTED')",id,expected)==0)throw stale();
 }
}
