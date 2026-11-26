package pe.tecsup.main.community;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicAdministration {
 private final JdbcTemplate db;private final IdentityService users;private final AdministrativeAudit audit;
 public AcademicAdministration(JdbcTemplate db,IdentityService users,AdministrativeAudit audit){this.db=db;this.users=users;this.audit=audit;}
 public record Course(long id,String name,long teacherId,String teacher,int version){}
 public record Input(long id,String name,String teacherUsername,Integer expectedVersion){}
 public record Student(String username,long studentId,String name){}
 public List<Course> courses(String actor,int offset,int limit){
  users.admin(actor);IdentityService.page(offset,limit);
  return db.query("SELECT c.id,c.nombre,c.docente_id,d.nombre,c.admin_version FROM curso c JOIN docente d ON d.id=c.docente_id ORDER BY c.id LIMIT ? OFFSET ?",
   (r,n)->new Course(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getInt(5)),limit,offset);
 }
 private Course course(long id){return db.query("SELECT c.id,c.nombre,c.docente_id,d.nombre,c.admin_version FROM curso c JOIN docente d ON d.id=c.docente_id WHERE c.id=?",
   (r,n)->new Course(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getInt(5)),id).stream().findFirst().orElseThrow(DomainException::missing);}
 @Transactional public Course save(String actor,long id,Input input,boolean create){
  users.admin(actor);
  if(id<1||id>9_007_199_254_740_991L||input.id()!=id)throw DomainException.invalid("Identificador de curso positivo requerido y coincidente.");
  String name=IdentityService.text(input.name(),100,"nombre del curso");
  var teacher=users.active(IdentityService.text(input.teacherUsername(),60,"usuario docente"));
  if(!teacher.role().equals("DOCENTE"))throw DomainException.invalid("El responsable debe ser una cuenta docente activa.");
  if(create){
   if(input.expectedVersion()!=null)throw DomainException.invalid("La creación no requiere versión previa.");
   db.update("INSERT INTO curso(id,nombre,docente_id,admin_version) VALUES(?,?,?,0)",id,name,teacher.docenteId());
  }else{
   if(db.queryForList("SELECT id FROM curso WHERE id=? FOR UPDATE",Long.class,id).isEmpty())throw DomainException.missing();
   course(id);
   if(input.expectedVersion()==null||input.expectedVersion()<0)throw DomainException.invalid("Actualiza el curso antes de editarlo.");
   // A reassignment must not silently conflict with an existing teacher timetable.
   if(db.queryForObject("SELECT COUNT(*) FROM timetable WHERE curso_id=?",Long.class,id)>0 && course(id).teacherId()!=teacher.docenteId())
    throw DomainException.invalid("Retira o ajusta primero el horario del curso antes de cambiar de docente.");
   int changed=db.update("UPDATE curso SET nombre=?,docente_id=?,admin_version=admin_version+1 WHERE id=? AND admin_version=?",name,teacher.docenteId(),id,input.expectedVersion());
   if(changed==0)throw new DomainException(HttpStatus.CONFLICT,"CONFLICT","El curso cambió. Actualiza antes de guardarlo.");
  }
  audit.record(actor,create?"COURSE_CREATED":"COURSE_UPDATED",Long.toString(id));return course(id);
 }
 public List<Student> students(String actor,long course,int offset,int limit){
  users.admin(actor);IdentityService.page(offset,limit);course(course);
  return db.query("SELECT u.username,a.id,a.nombre FROM alumno a JOIN matricula m ON m.alumno_id=a.id JOIN main_user u ON u.alumno_id=a.id WHERE m.curso_id=? ORDER BY a.id LIMIT ? OFFSET ?",
   (r,n)->new Student(r.getString(1),r.getLong(2),r.getString(3)),course,limit,offset);
 }
 @Transactional public void enroll(String actor,long course,String username,boolean enrolled){
  users.admin(actor);course(course);
  // The course lock serializes enrollment changes with grading and other enrollment changes.
  db.queryForObject("SELECT id FROM curso WHERE id=? FOR UPDATE",Long.class,course);
  var student=users.account(IdentityService.text(username,60,"usuario estudiante"));
  if(!student.role().equals("ALUMNO")||(enrolled&&!student.enabled()))throw DomainException.invalid("La matrícula requiere una cuenta de estudiante activa.");
  if(enrolled){
   if(db.queryForObject("SELECT COUNT(*) FROM matricula WHERE curso_id=? AND alumno_id=?",Long.class,course,student.alumnoId())==0){
    db.update("INSERT INTO matricula(alumno_id,curso_id) VALUES(?,?)",student.alumnoId(),course);audit.record(actor,"ENROLLMENT_GRANTED",course+":"+username);
   }
  }else if(db.update("DELETE FROM matricula WHERE curso_id=? AND alumno_id=?",course,student.alumnoId())>0)audit.record(actor,"ENROLLMENT_REVOKED",course+":"+username);
 }
 public List<AdministrativeAudit.Event> events(String actor,int offset,int limit){users.admin(actor);return audit.events(offset,limit);}
}
