package pe.tecsup.main.community;
import java.util.*;
import java.sql.Timestamp;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class NotificationService {
 private final JdbcTemplate db;private final IdentityService users;private final Clock clock;
 public NotificationService(JdbcTemplate db,IdentityService users,Clock clock){this.db=db;this.users=users;this.clock=clock;}
 public record Notice(String id,String kind,String conversationId,String createdAt,boolean read){}
 public void enqueue(String recipient,String sender,String conversation,String message,String kind,String event){
  if(recipient.equals(sender)||!users.account(recipient).enabled()||!users.preferences(recipient).notifications()||users.blocked(recipient,sender))return;
  if(db.queryForObject("SELECT COUNT(*) FROM contact_preference WHERE owner=? AND subject=? AND muted=TRUE",Long.class,recipient,sender)>0)return;
  if(db.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient=? AND event_key=?",Long.class,recipient,event)>0)return;
  db.update("INSERT INTO notification(id,recipient,sender,conversation_id,message_id,kind,event_key,created_at) VALUES(?,?,?,?,?,?,?,?)",
   UUID.randomUUID().toString(),recipient,sender,conversation,message,kind,event,Timestamp.valueOf(LocalDateTime.now(clock)));
 }
 private static final String VISIBLE="""
  FROM notification n JOIN main_user s ON s.username=n.sender
  JOIN conversation_member cm ON cm.conversation_id=n.conversation_id AND cm.username=n.recipient
  WHERE n.recipient=? AND s.enabled=TRUE
  AND ((n.kind='INVITATION' AND cm.state='INVITED') OR
       (n.kind='MESSAGE' AND cm.state='ACTIVE' AND EXISTS(SELECT 1 FROM message m WHERE m.id=n.message_id AND m.published_revision IS NOT NULL AND m.created_at>=cm.joined_at)))
  AND NOT EXISTS(SELECT 1 FROM contact_preference p WHERE
   (p.blocked=TRUE AND ((p.owner=n.recipient AND p.subject=n.sender) OR (p.owner=n.sender AND p.subject=n.recipient))) OR
   (p.muted=TRUE AND p.owner=n.recipient AND p.subject=n.sender))
  """;
 public List<Notice> list(String actor,int offset,int limit){
  users.active(actor);IdentityService.page(offset,limit);if(!users.preferences(actor).notifications())return List.of();
  return db.query("SELECT n.id,n.kind,n.conversation_id,n.created_at,n.read_at "+VISIBLE+" ORDER BY n.created_at DESC,n.id DESC LIMIT ? OFFSET ?",
   (r,n)->new Notice(r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4).toString(),r.getTimestamp(5)!=null),actor,limit,offset);
 }
 public void read(String actor,String id){
  users.active(actor);
  if(db.queryForObject("SELECT COUNT(*) "+VISIBLE+" AND n.id=?",Long.class,actor,id)==0)throw DomainException.missing();
  db.update("UPDATE notification SET read_at=COALESCE(read_at,?) WHERE recipient=? AND id=?",Timestamp.valueOf(LocalDateTime.now(clock)),actor,id);
 }
}
