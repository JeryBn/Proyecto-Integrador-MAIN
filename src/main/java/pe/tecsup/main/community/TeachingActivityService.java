package pe.tecsup.main.community;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingActivityService {
 private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
 private final JdbcTemplate db;private final IdentityService users;private final AdministrativeAudit audit;private final Clock clock;
 public TeachingActivityService(JdbcTemplate db,IdentityService users,AdministrativeAudit audit,Clock clock){this.db=db;this.users=users;this.audit=audit;this.clock=clock;}
 public record Activity(long id,String title,String description,String dueAt,int enrolled,int submitted,int drafts){}
 public record Input(String title,String description,String dueAt){}

 private void owns(String actor,long course,boolean lock){
  var user=users.active(actor);
  String sql="SELECT docente_id FROM curso WHERE id=?"+(lock?" FOR UPDATE":"");
  var teacher=db.queryForList(sql,Long.class,course);
  if(teacher.isEmpty())throw DomainException.missing();
  if(!user.role().equals("DOCENTE")||!Objects.equals(user.docenteId(),teacher.getFirst()))throw DomainException.denied();
 }
 public List<Activity> list(String actor,long course){
  owns(actor,course,false);
  return db.query("""
   SELECT a.id,a.titulo,a.descripcion,a.fecha_limite,
    (SELECT COUNT(*) FROM matricula m WHERE m.curso_id=a.curso_id) enrolled,
    (SELECT COUNT(*) FROM entrega e JOIN matricula m ON m.alumno_id=e.alumno_id AND m.curso_id=a.curso_id WHERE e.actividad_id=a.id AND e.status='SUBMITTED') submitted,
    (SELECT COUNT(*) FROM entrega e JOIN matricula m ON m.alumno_id=e.alumno_id AND m.curso_id=a.curso_id WHERE e.actividad_id=a.id AND e.status='DRAFT') drafts
   FROM actividad a WHERE a.curso_id=? ORDER BY a.fecha_limite,a.id
   """,(r,n)->new Activity(r.getLong(1),r.getString(2),r.getString(3),r.getTimestamp(4).toLocalDateTime().toString(),r.getInt(5),r.getInt(6),r.getInt(7)),course);
 }
 @Transactional public Activity create(String actor,long course,Input input){
  owns(actor,course,true);
  if(input==null)throw DomainException.invalid("Completa los datos de la actividad.");
  String title=IdentityService.text(input.title(),150,"título");
  String description=IdentityService.text(input.description(),2000,"descripción");
  LocalDateTime due;
  try{
   if(input.dueAt()==null||input.dueAt().length()!=19)throw new DateTimeParseException("Formato","",0);
   due=LocalDateTime.parse(input.dueAt(),DATE);
  }catch(DateTimeParseException exception){throw DomainException.invalid("La fecha límite debe usar YYYY-MM-DDTHH:mm:ss.");}
  if(!due.isAfter(LocalDateTime.now(clock)))throw DomainException.invalid("La fecha límite debe ser posterior al momento actual.");
  // The original academic schema uses explicit BIGINT identifiers. A global course lock
  // keeps allocation deterministic and safe until the shared schema adopts identities.
  db.queryForObject("SELECT id FROM curso ORDER BY id LIMIT 1 FOR UPDATE",Long.class);
  long id=db.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM actividad",Long.class);
  if(id<1||id>9_007_199_254_740_991L)throw DomainException.invalid("No se pudo asignar un identificador seguro.");
  db.update("INSERT INTO actividad(id,curso_id,titulo,descripcion,fecha_limite) VALUES(?,?,?,?,?)",id,course,title,description,Timestamp.valueOf(due));
  audit.record(actor,"ACTIVITY_CREATED",String.valueOf(id));
  return list(actor,course).stream().filter(item->item.id()==id).findFirst().orElseThrow(DomainException::missing);
 }
}
