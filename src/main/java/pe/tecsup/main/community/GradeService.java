package pe.tecsup.main.community;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class GradeService {
 private final JdbcTemplate db;private final IdentityService users;private final AcademicService academic;private final Clock clock;
 public GradeService(JdbcTemplate db,IdentityService users,AcademicService academic,Clock clock){this.db=db;this.users=users;this.academic=academic;this.clock=clock;}
 public record Grade(String id,String assessment,BigDecimal score,BigDecimal maximum,String recordedAt){}
 public record CourseGrades(long courseId,String course,String teacher,List<Grade> grades){}
 public record Input(long studentId,String assessment,BigDecimal score,BigDecimal maximum,String expectedRecordedAt){
  public Input(long studentId,String assessment,BigDecimal score,BigDecimal maximum){this(studentId,assessment,score,maximum,null);}
 }
 public List<CourseGrades> own(String actor){
  var user=users.active(actor);if(!user.role().equals("ALUMNO"))throw DomainException.denied();
  return academic.agenda(actor).courses().stream().map(c->new CourseGrades(c.id(),c.name(),c.teacher(),
   db.query("SELECT id,assessment,score,maximum,recorded_at FROM course_grade WHERE course_id=? AND student_id=? ORDER BY recorded_at,id",
    (r,n)->new Grade(r.getString(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getTimestamp(5).toString()),c.id(),user.alumnoId()))).toList();
 }
 public record StudentGrades(long studentId,String name,List<Grade> grades){}
 public List<StudentGrades> roster(String actor,long course){
  var user=users.active(actor);
  var teacher=db.queryForList("SELECT docente_id FROM curso WHERE id=?",Long.class,course);
  if(teacher.isEmpty())throw DomainException.missing();
  if(!user.role().equals("DOCENTE")||!Objects.equals(user.docenteId(),teacher.getFirst()))throw DomainException.denied();
  return db.query("SELECT a.id,a.nombre FROM alumno a JOIN matricula m ON m.alumno_id=a.id WHERE m.curso_id=? ORDER BY a.nombre,a.id",
   (r,n)->new StudentGrades(r.getLong(1),r.getString(2),db.query("SELECT id,assessment,score,maximum,recorded_at FROM course_grade WHERE course_id=? AND student_id=? ORDER BY assessment,id",
    (g,i)->new Grade(g.getString(1),g.getString(2),g.getBigDecimal(3),g.getBigDecimal(4),g.getTimestamp(5).toString()),course,r.getLong(1))),course);
 }
 @Transactional public Grade save(String actor,long course,Input input){
  var user=users.active(actor);
  var teacher=db.queryForList("SELECT docente_id FROM curso WHERE id=? FOR UPDATE",Long.class,course);
  if(teacher.isEmpty())throw DomainException.missing();
  if(!user.role().equals("ADMIN")&&!(user.role().equals("DOCENTE")&&Objects.equals(user.docenteId(),teacher.getFirst())))throw DomainException.denied();
  if(db.queryForObject("SELECT COUNT(*) FROM matricula WHERE curso_id=? AND alumno_id=?",Long.class,course,input.studentId())==0)throw DomainException.missing();
  String assessment=IdentityService.text(input.assessment(),120,"evaluación");
  var score=input.score();var maximum=input.maximum();
  if(score==null||maximum==null||maximum.signum()<=0||maximum.compareTo(new BigDecimal("1000"))>0||score.signum()<0||score.compareTo(maximum)>0||score.stripTrailingZeros().scale()>2||maximum.stripTrailingZeros().scale()>2)
   throw DomainException.invalid("Nota entre 0 y la escala máxima (hasta 1000), con máximo dos decimales.");
  var previous=db.queryForList("SELECT id FROM course_grade WHERE course_id=? AND student_id=? AND assessment=?",String.class,course,input.studentId(),assessment);
  if(input.expectedRecordedAt()!=null){
   String actual=previous.isEmpty()?"NEW":db.queryForObject("SELECT recorded_at FROM course_grade WHERE id=?",Timestamp.class,previous.getFirst()).toString();
   if(!actual.equals(input.expectedRecordedAt()))throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","La nota cambió. Actualiza el curso antes de guardarla.");
  }
  String id=previous.isEmpty()?UUID.randomUUID().toString():previous.getFirst();var now=Timestamp.valueOf(LocalDateTime.now(clock));
  if(previous.isEmpty())db.update("INSERT INTO course_grade VALUES(?,?,?,?,?,?,?,?)",id,course,input.studentId(),assessment,score,maximum,actor,now);
  else db.update("UPDATE course_grade SET score=?,maximum=?,recorded_by=?,recorded_at=? WHERE id=?",score,maximum,actor,now,id);
  return new Grade(id,assessment,score,maximum,now.toString());
 }
}
