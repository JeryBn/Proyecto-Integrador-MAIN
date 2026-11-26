package pe.tecsup.main.community;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingSchoolService {
 private final JdbcTemplate db;private final IdentityService users;private final AdministrativeAudit audit;private final Clock clock;
 public TeachingSchoolService(JdbcTemplate db,IdentityService users,AdministrativeAudit audit,Clock clock){this.db=db;this.users=users;this.audit=audit;this.clock=clock;}
 public record Scope(String id,long courseId,String course,String classroomId,int yearId,String level,int grade,String section,String mode,BigDecimal maximum,String activePeriodId) {}
 public record Student(String enrollmentId,long studentId,String name) {}
 public record Mark(String id,String enrollmentId,String student,String assessment,String competency,BigDecimal score,String achievement,int version) {}
 public record Sheet(Scope scope,List<FamilySchoolService.Period> periods,List<Student> students,List<Mark> marks) {}
 public record Input(String enrollmentId,String periodId,String assessment,String competency,BigDecimal score,String achievement,Integer expectedVersion) {}
 public List<Scope> scopes(String actor) {
  if(!users.active(actor).role().equals("DOCENTE"))throw DomainException.denied();
  return db.query("SELECT s.id,c.id,c.nombre,r.id,r.year_id,r.level,r.grade,r.section,r.grading_mode,r.maximum,y.active_period_id FROM school_course s JOIN curso c ON c.id=s.course_id JOIN school_classroom r ON r.id=s.classroom_id JOIN school_year y ON y.id=r.year_id WHERE s.teacher_username=? ORDER BY r.year_id DESC,r.level,r.grade,r.section,c.nombre",
   (r,n)->new Scope(r.getString(1),r.getLong(2),r.getString(3),r.getString(4),r.getInt(5),r.getString(6),r.getInt(7),r.getString(8),r.getString(9),r.getBigDecimal(10),r.getString(11)),actor);
 }
 private Scope scope(String actor,String id){return scopes(actor).stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(DomainException::denied);}
 public Sheet sheet(String actor,String assignment,String periodId) {
  var scope=scope(actor,assignment);
  var periods=db.query("SELECT id,year_id,name,position FROM school_period WHERE year_id=? ORDER BY position",(r,n)->new FamilySchoolService.Period(r.getString(1),r.getInt(2),r.getString(3),r.getInt(4)),scope.yearId());
  if(periodId!=null&&periods.stream().noneMatch(p->p.id().equals(periodId)))throw DomainException.invalid("El periodo no pertenece al año del aula.");
  var students=db.query("SELECT e.id,e.student_id,a.nombre FROM school_enrollment e JOIN alumno a ON a.id=e.student_id WHERE e.classroom_id=? ORDER BY a.nombre,a.id",(r,n)->new Student(r.getString(1),r.getLong(2),r.getString(3)),scope.classroomId());
  var marks=periodId==null?List.<Mark>of():db.query("SELECT m.id,m.enrollment_id,a.nombre,m.assessment,m.competency,m.score,m.achievement,m.version FROM school_mark m JOIN school_enrollment e ON e.id=m.enrollment_id JOIN alumno a ON a.id=e.student_id WHERE m.assignment_id=? AND m.period_id=? AND e.classroom_id=? ORDER BY a.nombre,m.competency,m.recorded_at,m.id",
   (r,n)->new Mark(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getString(7),r.getInt(8)),assignment,periodId,scope.classroomId());
  return new Sheet(scope,periods,students,marks);
 }
 @Transactional public Map<String,Object> save(String actor,String assignment,Input input) {
  var scope=scope(actor,assignment);
  db.queryForObject("SELECT id FROM school_year WHERE id=? FOR UPDATE",Integer.class,scope.yearId());
  var sheet=sheet(actor,assignment,input.periodId());
  var period=sheet.periods().stream().filter(p->p.id().equals(input.periodId())).findFirst().orElseThrow(()->DomainException.invalid("Selecciona el periodo."));
  var student=sheet.students().stream().filter(s->s.enrollmentId().equals(input.enrollmentId())).findFirst().orElseThrow(DomainException::denied);
  String assessment=IdentityService.text(input.assessment(),80,"evaluación"),competency=IdentityService.text(input.competency(),120,"competencia");
  BigDecimal score=null;String achievement=null;
  if(scope.mode().equals("NUMERIC")) {
   score=input.score();if(score==null||score.signum()<0||score.compareTo(scope.maximum())>0||score.stripTrailingZeros().scale()>2)throw DomainException.invalid("La nota debe estar entre 0 y la escala del aula, con hasta dos decimales.");
  } else {
   achievement=input.achievement();if(achievement==null||!Set.of("AD","A","B","C").contains(achievement))throw DomainException.invalid("Selecciona AD, A, B o C.");
  }
  var previous=sheet.marks().stream().filter(m->m.enrollmentId().equals(student.enrollmentId())&&m.assessment().equals(assessment)).findFirst();
  int version=previous.isPresent()?previous.get().version():-1;
  if(input.expectedVersion()==null||input.expectedVersion()!=version)throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","La evaluación ya existe o cambió. Actualiza y usa Editar.");
  String id=previous.isPresent()?previous.get().id():UUID.randomUUID().toString();var now=Timestamp.valueOf(LocalDateTime.now(clock));
  if(previous.isPresent())db.update("UPDATE school_mark SET score=?,achievement=?,competency=?,recorded_by=?,recorded_at=?,version=version+1 WHERE id=?",score,achievement,competency,actor,now,id);
  else db.update("INSERT INTO school_mark(id,enrollment_id,assignment_id,period_id,assessment,score,achievement,recorded_by,recorded_at,version,competency) VALUES(?,?,?,?,?,?,?,?,?,0,?)",id,student.enrollmentId(),assignment,period.id(),assessment,score,achievement,actor,now,competency);
  if(score!=null) {
   if(db.queryForObject("SELECT COUNT(*) FROM course_grade WHERE id=?",Long.class,id)>0)db.update("UPDATE course_grade SET score=?,maximum=?,recorded_by=?,recorded_at=? WHERE id=?",score,scope.maximum(),actor,now,id);
   else db.update("INSERT INTO course_grade VALUES(?,?,?,?,?,?,?,?)",id,scope.courseId(),student.studentId(),scope.yearId()+"/"+period.position()+" - "+assessment,score,scope.maximum(),actor,now);
  }
  audit.record(actor,previous.isPresent()?"SCHOOL_MARK_UPDATED":"SCHOOL_MARK_CREATED",id);
  return Map.of("id",id,"version",version+1);
 }
}
