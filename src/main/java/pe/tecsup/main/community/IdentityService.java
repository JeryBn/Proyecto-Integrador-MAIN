package pe.tecsup.main.community;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService implements UserDetailsService {
 private final JdbcTemplate db;private final PasswordEncoder encoder;private final SessionRegistry sessions;private final Clock clock;private final AdministrativeAudit audit;
 public IdentityService(JdbcTemplate db,PasswordEncoder encoder,SessionRegistry sessions,Clock clock,AdministrativeAudit audit){this.db=db;this.encoder=encoder;this.sessions=sessions;this.clock=clock;this.audit=audit;}
 public record Account(String username,String name,String role,boolean enabled,Long alumnoId,Long docenteId){}
 public record NewAccount(String username,String name,String role,String password,Long alumnoId,Long docenteId){
  @Override public String toString(){return "NewAccount[redacted]";}
 }
 public record Preferences(boolean darkTheme,boolean notifications,String statusAudience){}
 public record Contact(String username,String name,String role,boolean muted,boolean blocked){}
 public Account account(String username){
  return db.query("SELECT * FROM main_user WHERE username=?",(r,n)->new Account(r.getString("username"),r.getString("display_name"),r.getString("role"),
   r.getBoolean("enabled"),r.getObject("alumno_id",Long.class),r.getObject("docente_id",Long.class)),username).stream().findFirst().orElseThrow(DomainException::missing);
 }
 public Account active(String username){var a=account(username);if(!a.enabled())throw DomainException.denied();return a;}
 public void admin(String username){if(!active(username).role().equals("ADMIN"))throw DomainException.denied();}
 @Override public UserDetails loadUserByUsername(String username){
  var rows=db.query("SELECT * FROM main_user WHERE username=?",(r,n)->User.withUsername(r.getString("username")).password(r.getString("password_hash"))
   .roles(r.getString("role")).disabled(!r.getBoolean("enabled")).build(),username);
  if(rows.isEmpty())throw new UsernameNotFoundException("Cuenta no disponible.");return rows.getFirst();
 }
 public Map<String,String> profile(String username){
  var a=active(username);String role=switch(a.role()){case "ALUMNO"->"Alumno";case "DOCENTE"->"Docente";case "FAMILIAR"->"Familiar";default->"ADMIN";};
  return Map.of("usuario",a.username(),"nombre",a.name(),"rol",role);
 }
 public static String text(String value,int max,String field){
  if(value==null||value.isBlank()||value.strip().length()>max)throw DomainException.invalid(field+": texto de 1 a "+max+" caracteres.");return value.strip();
 }
 private void password(String value){
  if(value==null||value.length()<12||value.getBytes(StandardCharsets.UTF_8).length>72)
   throw DomainException.invalid("Contraseña: al menos 12 caracteres y como máximo 72 bytes UTF-8.");
 }
 private Long academicId(Long id){if(id==null||id<1||id>9_007_199_254_740_991L)throw DomainException.invalid("Identificador académico positivo requerido.");return id;}
 @Transactional public Account create(String actor,NewAccount input){
  admin(actor);String username=text(input.username(),60,"usuario"),name=text(input.name(),100,"nombre");password(input.password());
  if(!username.matches("[a-zA-Z0-9._-]{3,60}"))throw DomainException.invalid("Usuario: 3 a 60 letras, números, punto, guion o guion bajo.");
  if(input.role()==null||!Set.of("ALUMNO","DOCENTE","FAMILIAR").contains(input.role()))throw DomainException.invalid("Rol inválido.");
  if(db.queryForObject("SELECT COUNT(*) FROM main_user WHERE username=?",Long.class,username)>0)
   throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","Usuario existente.");
  Long student=null,teacher=null;
  if(input.role().equals("ALUMNO")){
   student=academicId(input.alumnoId());var owners=db.queryForList("SELECT usuario FROM alumno WHERE id=?",String.class,student);
   if(owners.isEmpty())db.update("INSERT INTO alumno(id,usuario,nombre) VALUES(?,?,?)",student,username,name);
   else if(!owners.getFirst().equals(username))throw DomainException.invalid("El registro académico pertenece a otro usuario.");
   if(input.docenteId()!=null)throw DomainException.invalid("Un alumno no puede reclamar identidad docente.");
  }else if(input.role().equals("DOCENTE")){
   teacher=academicId(input.docenteId());
   if(db.queryForObject("SELECT COUNT(*) FROM docente WHERE id=?",Long.class,teacher)==0)db.update("INSERT INTO docente(id,nombre) VALUES(?,?)",teacher,name);
   if(input.alumnoId()!=null)throw DomainException.invalid("Un docente no puede reclamar identidad de alumno.");
  }else if(input.alumnoId()!=null||input.docenteId()!=null)throw DomainException.invalid("Los parentescos se verifican por separado.");
  db.update("INSERT INTO main_user(username,display_name,role,password_hash,enabled,alumno_id,docente_id) VALUES(?,?,?,?,TRUE,?,?)",
   username,name,input.role(),encoder.encode(input.password()),student,teacher);audit.record(actor,"ACCOUNT_CREATED",username);return account(username);
 }
 @Transactional public void bootstrap(String username,String name,String role,String hash,Long student,Long teacher){
  if(db.queryForObject("SELECT COUNT(*) FROM main_user WHERE username=?",Long.class,username)==0)
   db.update("INSERT INTO main_user(username,display_name,role,password_hash,enabled,alumno_id,docente_id) VALUES(?,?,?,?,TRUE,?,?)",username,name,role,hash,student,teacher);
  else{
   if(!account(username).role().equals(role))throw new IllegalStateException("Colisión de rol en la cuenta de arranque.");
   if(role.equals("ADMIN"))db.update("UPDATE main_user SET password_hash=?,enabled=TRUE WHERE username=?",hash,username);
  }
 }
 public static void page(int offset,int limit){if(offset<0||offset>1_000_000||limit<1||limit>100)throw DomainException.invalid("Paginación: offset 0..1000000; limit 1..100.");}
 public List<Account> users(String actor,int offset,int limit){
  admin(actor);page(offset,limit);return db.query("SELECT username FROM main_user ORDER BY username LIMIT ? OFFSET ?",(r,n)->account(r.getString(1)),limit,offset);
 }
 @Transactional public void verifyGuardian(String actor,String guardian,long student){
  admin(actor);if(!active(guardian).role().equals("FAMILIAR"))throw DomainException.invalid("La cuenta debe ser familiar.");
  if(db.queryForObject("SELECT COUNT(*) FROM alumno WHERE id=?",Long.class,student)==0)throw DomainException.missing();
  if(db.queryForObject("SELECT COUNT(*) FROM guardian_link WHERE guardian=? AND alumno_id=?",Long.class,guardian,student)==0){
   db.update("INSERT INTO guardian_link VALUES(?,?,?,?)",guardian,student,actor,Timestamp.valueOf(LocalDateTime.now(clock)));
   audit.record(actor,"GUARDIAN_VERIFIED",guardian+":"+student);
  }
 }
 @Transactional public void revokeGuardian(String actor,String guardian,long student){admin(actor);if(db.update("DELETE FROM guardian_link WHERE guardian=? AND alumno_id=?",guardian,student)>0)audit.record(actor,"GUARDIAN_REVOKED",guardian+":"+student);}
 @Transactional public void resetPassword(String actor,String target,String value){
  admin(actor);password(value);if(active(target).role().equals("ADMIN"))throw DomainException.denied();
  db.update("UPDATE main_user SET password_hash=? WHERE username=?",encoder.encode(value),target);audit.record(actor,"PASSWORD_RESET",target);expire(target);
 }
 @Transactional public void disable(String actor,String target){
  admin(actor);if(account(target).role().equals("ADMIN"))throw DomainException.denied();
  db.update("UPDATE main_user SET enabled=FALSE WHERE username=?",target);audit.record(actor,"ACCOUNT_DISABLED",target);expire(target);
 }
 private void expire(String username){
  for(Object p:sessions.getAllPrincipals())if(p instanceof UserDetails u&&u.getUsername().equals(username))
   sessions.getAllSessions(p,false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
 }
 public Preferences preferences(String username){
  active(username);return db.queryForObject("SELECT dark_theme,notifications,status_audience FROM main_user WHERE username=?",
   (r,n)->new Preferences(r.getBoolean(1),r.getBoolean(2),r.getString(3)),username);
 }
 public Preferences preferences(String username,Preferences input){
  active(username);if(!Set.of("CONTACTS","PRIVATE").contains(Objects.toString(input.statusAudience(),"")))throw DomainException.invalid("Audiencia: CONTACTS o PRIVATE.");
  db.update("UPDATE main_user SET dark_theme=?,notifications=?,status_audience=? WHERE username=?",input.darkTheme(),input.notifications(),input.statusAudience(),username);
  return preferences(username);
 }
 @Transactional public void approveContact(String actor,String a,String b){
  a=text(a,60,"primer usuario");b=text(b,60,"segundo usuario");
  admin(actor);if(a.equals(b)||active(a).role().equals("ADMIN")||active(b).role().equals("ADMIN"))throw DomainException.invalid("Contacto inválido.");
  String first=a.compareTo(b)<0?a:b,second=a.compareTo(b)<0?b:a;
  if(db.queryForObject("SELECT COUNT(*) FROM contact_link WHERE user_a=? AND user_b=?",Long.class,first,second)==0){db.update("INSERT INTO contact_link VALUES(?,?,?)",first,second,actor);audit.record(actor,"CONTACT_APPROVED",first+":"+second);}
 }
 public boolean contacts(String a,String b){
  String first=a.compareTo(b)<0?a:b,second=a.compareTo(b)<0?b:a;
  return db.queryForObject("SELECT COUNT(*) FROM contact_link WHERE user_a=? AND user_b=?",Long.class,first,second)>0;
 }
 public boolean blocked(String a,String b){
  return db.queryForObject("SELECT COUNT(*) FROM contact_preference WHERE blocked=TRUE AND ((owner=? AND subject=?) OR (owner=? AND subject=?))",Long.class,a,b,b,a)>0;
 }
 public List<Contact> contacts(String actor){
  active(actor);return db.query("""
   SELECT u.username,u.display_name,u.role,COALESCE(p.muted,FALSE) muted,COALESCE(p.blocked,FALSE) blocked
   FROM main_user u JOIN contact_link c ON (c.user_a=? AND c.user_b=u.username) OR (c.user_b=? AND c.user_a=u.username)
   LEFT JOIN contact_preference p ON p.owner=? AND p.subject=u.username WHERE u.enabled=TRUE ORDER BY u.display_name,u.username
   """,(r,n)->new Contact(r.getString(1),r.getString(2),r.getString(3),r.getBoolean(4),r.getBoolean(5)),actor,actor,actor);
 }
 @Transactional public void contactPreference(String actor,String target,boolean muted,boolean blocked){
  active(actor);active(target);if(!contacts(actor,target))throw DomainException.missing();
  db.queryForObject("SELECT username FROM main_user WHERE username=? FOR UPDATE",String.class,actor);
  if(db.update("UPDATE contact_preference SET muted=?,blocked=? WHERE owner=? AND subject=?",muted,blocked,actor,target)==0)
   db.update("INSERT INTO contact_preference VALUES(?,?,?,?)",actor,target,muted,blocked);
 }
 public String report(String actor,String target,String reason){
  active(actor);if(!contacts(actor,target))throw DomainException.missing();String id=UUID.randomUUID().toString();
  db.update("INSERT INTO contact_report VALUES(?,?,?,?,?)",id,actor,target,text(reason,1000,"motivo"),Timestamp.valueOf(LocalDateTime.now(clock)));return id;
 }
 public List<Map<String,Object>> reports(String actor,int offset,int limit){
  admin(actor);page(offset,limit);return db.queryForList("SELECT id,reporter,subject,reason,created_at FROM contact_report ORDER BY created_at DESC,id LIMIT ? OFFSET ?",limit,offset);
 }
}
