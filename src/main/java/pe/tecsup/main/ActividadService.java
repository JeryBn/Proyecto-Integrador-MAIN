package pe.tecsup.main;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ActividadService {
 private final JdbcTemplate db;
 private final Clock clock;
 public ActividadService(JdbcTemplate db, Clock clock){this.db=db;this.clock=clock;}
 public record Actividad(long id,String titulo,String descripcion,String curso,String docente,LocalDateTime fechaLimite,boolean vencida,boolean completada){}
 public record Entrega(boolean saved,boolean submitted,String status,String note,String evidenceUrl,LocalDateTime submittedAt,LocalDateTime updatedAt,int revision){}
 public record EntregaRequest(String note,String evidenceUrl,String status,Integer expectedRevision){}
 private static final String SELECT = """
 SELECT a.id,a.titulo,a.descripcion,a.fecha_limite,c.nombre curso,d.nombre docente,
 CASE WHEN e.actividad_id IS NULL THEN FALSE ELSE TRUE END completada
 FROM actividad a JOIN curso c ON c.id=a.curso_id JOIN docente d ON d.id=c.docente_id
 JOIN matricula m ON m.curso_id=c.id JOIN alumno al ON al.id=m.alumno_id
 LEFT JOIN entrega e ON e.actividad_id=a.id AND e.alumno_id=al.id AND e.status='SUBMITTED'
 WHERE al.usuario=?
 """;
 boolean estaVencida(LocalDateTime fecha,boolean completada){
  return !completada && fecha.isBefore(LocalDateTime.now(clock));
 }
 private Actividad map(java.sql.ResultSet r) throws java.sql.SQLException{
  var fecha=r.getTimestamp("fecha_limite").toLocalDateTime();
  boolean completada=r.getBoolean("completada");
  return new Actividad(r.getLong("id"),r.getString("titulo"),r.getString("descripcion"),r.getString("curso"),r.getString("docente"),fecha,!completada&&estaVencida(fecha,completada),completada);
 }
 public List<Actividad> pendientes(String usuario){
  return db.query(SELECT+" AND e.actividad_id IS NULL ORDER BY a.fecha_limite,a.id",(r,n)->map(r),usuario);
 }
 public List<Actividad> todas(String usuario){
  return db.query(SELECT+" ORDER BY a.fecha_limite,a.id",(r,n)->map(r),usuario);
 }
 public Actividad detalle(String usuario,long id){
  return db.query(SELECT+" AND a.id=?",(r,n)->map(r),usuario,id).stream().findFirst()
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible"));
 }
 public Map<String,String> perfil(String usuario){
  return db.query("SELECT nombre FROM alumno WHERE usuario=?",(r,n)->Map.of("usuario",usuario,"nombre",r.getString("nombre"),"rol","Alumno"),usuario).stream().findFirst().orElseThrow();
 }
 public Entrega entrega(String usuario,long activityId){
  detalle(usuario,activityId);
  return db.query("""
    SELECT e.student_note,e.evidence_url,e.status,e.fecha,e.updated_at,e.revision
    FROM entrega e JOIN alumno a ON a.id=e.alumno_id
    WHERE a.usuario=? AND e.actividad_id=?
    """,(r,n)->{
      String status=r.getString("status");
      boolean submitted="SUBMITTED".equals(status);
      return new Entrega(true,submitted,status,r.getString("student_note"),r.getString("evidence_url"),
        submitted?r.getTimestamp("fecha").toLocalDateTime():null,r.getTimestamp("updated_at").toLocalDateTime(),r.getInt("revision"));
    },usuario,activityId).stream().findFirst().orElse(new Entrega(false,false,"NONE","","",null,null,0));
 }
 @Transactional
 public Entrega guardarEntrega(String usuario,long activityId,EntregaRequest request){
  detalle(usuario,activityId);
  String note=request==null||request.note()==null?"":request.note().trim();
  String evidence=request==null||request.evidenceUrl()==null?"":request.evidenceUrl().trim();
  String status=request==null||request.status()==null?"SUBMITTED":request.status().trim().toUpperCase(Locale.ROOT);
  int expected=request==null||request.expectedRevision()==null?0:request.expectedRevision();
  if(!Set.of("DRAFT","SUBMITTED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El estado de la entrega no es válido.");
  if(note.isEmpty()&&evidence.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Incluye una nota o enlace de evidencia.");
  if(note.length()>2000||evidence.length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La entrega supera el límite permitido.");
  if(!evidence.isEmpty()&&!evidence.matches("https?://[^\\s]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace debe usar HTTP o HTTPS.");
  Long studentId=db.query("SELECT id FROM alumno WHERE usuario=?",(r,n)->r.getLong(1),usuario).stream().findFirst()
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no disponible"));
  var current=entrega(usuario,activityId);
  LocalDateTime now=LocalDateTime.now(clock);
  if(!current.saved()){
   if(expected!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"La entrega cambió. Actualiza antes de guardar.");
   try{
    db.update("INSERT INTO entrega(alumno_id,actividad_id,fecha,student_note,evidence_url,status,updated_at,revision) VALUES(?,?,?,?,?,?,?,1)",studentId,activityId,now,note,evidence,status,now);
   }catch(DuplicateKeyException duplicate){
    throw new ResponseStatusException(HttpStatus.CONFLICT,"La entrega cambió. Actualiza antes de guardar.");
   }
  }else{
   if(expected!=current.revision())throw new ResponseStatusException(HttpStatus.CONFLICT,"La entrega cambió. Actualiza antes de guardar.");
   LocalDateTime submittedAt=!current.submitted()&&"SUBMITTED".equals(status)?now:current.submittedAt();
   int changed=db.update("UPDATE entrega SET student_note=?,evidence_url=?,status=?,fecha=?,updated_at=?,revision=revision+1 WHERE alumno_id=? AND actividad_id=? AND revision=?",note,evidence,status,submittedAt==null?now:submittedAt,now,studentId,activityId,expected);
   if(changed!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"La entrega cambió. Actualiza antes de guardar.");
  }
  return entrega(usuario,activityId);
 }
}
