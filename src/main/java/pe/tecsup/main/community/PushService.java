package pe.tecsup.main.community;
import java.util.*;import java.sql.Timestamp;import java.time.*;import java.security.MessageDigest;import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionRegistry;import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.event.EventListener;import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.transaction.annotation.Transactional;import org.springframework.dao.DuplicateKeyException;
@Service
public class PushService {
 private final JdbcTemplate db;private final IdentityService users;private final NotificationService notices;
 private final SessionRegistry sessions;private final PushTokenCipher cipher;private final FcmGateway gateway;private final boolean enabled;private final Clock clock;
 public PushService(JdbcTemplate db,IdentityService users,NotificationService notices,SessionRegistry sessions,PushTokenCipher cipher,FcmGateway gateway,@Value("${MAIN_PUSH_ENABLED:false}") boolean enabled,Clock clock){this.db=db;this.users=users;this.notices=notices;this.sessions=sessions;this.cipher=cipher;this.gateway=gateway;this.enabled=enabled;this.clock=clock;}
 public boolean configured(){return enabled&&cipher.ready()&&gateway.ready();}
 public static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}}
 private Timestamp now(){return Timestamp.valueOf(LocalDateTime.now(clock));}
 public record Registration(String deviceId,String token){}
 @Transactional public Map<String,String> register(String owner,String session,int timeout,Registration value){
  users.active(owner);if(!configured())throw new DomainException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"PUSH_NOT_CONFIGURED","Push aún no está configurado en el servidor.");
  if(value==null||value.deviceId()==null||!value.deviceId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")||value.token()==null||value.token().length()<20||value.token().length()>4096||value.token().chars().anyMatch(Character::isWhitespace))throw DomainException.invalid("Registro de dispositivo inválido.");
  String id=UUID.fromString(value.deviceId()).toString();String hash=digest(session);
  var existing=db.query("SELECT owner FROM push_device WHERE id=?",(r,n)->r.getString(1),id);
  if(!existing.isEmpty()&&!existing.getFirst().equals(owner))throw DomainException.denied();
  var previous=db.query("SELECT id,owner FROM push_device WHERE token_hash=? AND id<>?",(r,n)->new String[]{r.getString(1),r.getString(2)},digest(value.token()),id);
  for(var device:previous){if(!device[1].equals(owner))throw DomainException.invalid("El teléfono conserva otro registro. Cierra esa sesión o espera su caducidad antes de reintentar.");remove(owner,device[0]);}
  if(existing.isEmpty()&&db.queryForObject("SELECT COUNT(*) FROM push_device WHERE owner=?",Integer.class,owner)>=10)throw DomainException.invalid("Se alcanzó el límite de teléfonos registrados para esta cuenta.");
  String binding=UUID.randomUUID().toString();
  var same=db.query("SELECT binding FROM push_device WHERE id=? AND owner=? AND session_hash=?",(r,n)->r.getString(1),id,owner,hash);
  if(!same.isEmpty())binding=same.getFirst();
  String encrypted=cipher.encrypt(value.token(),id);Timestamp expiry=Timestamp.valueOf(LocalDateTime.now(clock).plusHours(24));
  if(existing.isEmpty())db.update("INSERT INTO push_device(id,owner,token_hash,token_cipher,binding,session_hash,session_timeout,registered_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?)",id,owner,digest(value.token()),encrypted,binding,hash,Math.min(86400,Math.max(60,timeout)),now(),expiry);
  else db.update("UPDATE push_device SET token_hash=?,token_cipher=?,binding=?,session_hash=?,session_timeout=?,expires_at=? WHERE id=? AND owner=?",digest(value.token()),encrypted,binding,hash,Math.min(86400,Math.max(60,timeout)),expiry,id,owner);
  return Map.of("binding",binding);
 }
 @Transactional public void remove(String owner,String id){db.update("DELETE FROM push_delivery WHERE device_id IN (SELECT id FROM push_device WHERE id=? AND owner=?)",id,owner);db.update("DELETE FROM push_device WHERE id=? AND owner=?",id,owner);}
 @EventListener public void destroyed(HttpSessionDestroyedEvent event){
  String hash=digest(event.getId());var devices=db.query("SELECT id,owner FROM push_device WHERE session_hash=?",(r,n)->new String[]{r.getString(1),r.getString(2)},hash);
  for(var device:devices)remove(device[1],device[0]);
 }
 private boolean sessionActive(String owner,String hash,int timeout){
  for(Object principal:sessions.getAllPrincipals()){
   String username=principal instanceof UserDetails u?u.getUsername():principal.toString();if(!owner.equals(username))continue;
   for(var info:sessions.getAllSessions(principal,false))if(digest(info.getSessionId()).equals(hash)&&info.getLastRequest().toInstant().plusSeconds(timeout).isAfter(clock.instant()))return true;
  }return false;
 }
 @Scheduled(fixedDelayString="${main.push.interval-ms:15000}") public synchronized void dispatch(){
  if(!configured())return;
  var expired=db.query("SELECT id,owner FROM push_device WHERE expires_at<=?",(r,n)->new String[]{r.getString(1),r.getString(2)},now());for(var device:expired)remove(device[1],device[0]);
  var candidates=db.query("SELECT d.id,d.owner,d.token_cipher,d.binding,d.session_hash,d.session_timeout,n.id FROM push_device d JOIN notification n ON n.recipient=d.owner LEFT JOIN push_delivery p ON p.device_id=d.id AND p.notice_id=n.id WHERE n.created_at>=d.registered_at AND n.read_at IS NULL AND n.created_at>=? AND (p.notice_id IS NULL OR (p.attempts<5 AND p.next_attempt<=? AND (p.state='PENDING' OR (p.state='SENDING' AND p.lease_until<?)))) ORDER BY n.created_at LIMIT 100",(r,n)->new Candidate(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),r.getString(7)),Timestamp.valueOf(LocalDateTime.now(clock).minusHours(24)),now(),now());
  for(var candidate:candidates)deliver(candidate);
 }
 private record Candidate(String device,String owner,String token,String binding,String session,int timeout,String notice){}
 private void deliver(Candidate c){
  try{
   if(!sessionActive(c.owner,c.session,c.timeout)){remove(c.owner,c.device);return;}
   if(!notices.pushVisible(c.owner,c.notice))return;
   try{db.update("INSERT INTO push_delivery(device_id,notice_id,state,attempts,next_attempt) VALUES(?,?,'PENDING',0,?)",c.device,c.notice,now());}catch(DuplicateKeyException ignored){}
   Timestamp lease=Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(2));
   int claimed=db.update("UPDATE push_delivery SET state='SENDING',attempts=attempts+1,lease_until=? WHERE device_id=? AND notice_id=? AND attempts<5 AND next_attempt<=? AND (state='PENDING' OR (state='SENDING' AND lease_until<?))",lease,c.device,c.notice,now(),now());
   if(claimed==0)return;
   if(!notices.pushVisible(c.owner,c.notice)){db.update("UPDATE push_delivery SET state='SKIPPED' WHERE device_id=? AND notice_id=?",c.device,c.notice);return;}
   var result=gateway.send(cipher.decrypt(c.token,c.device),c.binding,digest(c.device+":"+c.notice));
   if(result==FcmGateway.Outcome.INVALID_TOKEN){remove(c.owner,c.device);return;}
   db.update("UPDATE push_delivery SET state=?,next_attempt=?,lease_until=NULL WHERE device_id=? AND notice_id=?",result==FcmGateway.Outcome.ACCEPTED?"ACCEPTED":"PENDING",Timestamp.valueOf(LocalDateTime.now(clock).plusMinutes(2)),c.device,c.notice);
  }catch(DomainException ignored){/* Disabled or revoked accounts never receive content. */}
 }
}
