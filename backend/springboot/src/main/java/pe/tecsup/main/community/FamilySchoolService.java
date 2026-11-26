package pe.tecsup.main.community;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Every read checks the current verified relationship, including PDF downloads. */
@Service
public class FamilySchoolService {
 private final JdbcTemplate db;
 private final IdentityService users;
 public FamilySchoolService(JdbcTemplate db, IdentityService users) { this.db=db; this.users=users; }
 public record Student(long id,String name) {}
 public record Year(int id,String name,String activePeriodId,String classroomId,String level,int grade,String section) {}
 public record Period(String id,int yearId,String name,int position) {}
 public record Course(long id,String name) {}
 public record Catalog(Student student,List<Year> years,List<Period> periods) {}
 public record Mark(String id,long courseId,String course,String competency,String assessment,
                    BigDecimal score,BigDecimal maximum,String achievement,String recordedAt) {}
 public record Report(Student student,Year year,Period period,List<Course> courses,List<Mark> marks) {}
 public record Attendance(String date,String status,String note) {}

 public List<Student> students(String actor) {
  if(!users.active(actor).role().equals("FAMILIAR")) throw DomainException.denied();
  return db.query("SELECT a.id,a.nombre FROM alumno a JOIN guardian_link g ON g.alumno_id=a.id WHERE g.guardian=? ORDER BY a.nombre,a.id",
    (r,n)->new Student(r.getLong(1),r.getString(2)),actor);
 }
 public Student requireStudent(String actor,long student) {
  return students(actor).stream().filter(s->s.id()==student).findFirst().orElseThrow(DomainException::denied);
 }
 public Catalog catalog(String actor,long student) {
  var linked=requireStudent(actor,student);
  return catalogForStudent(linked);
 }
 public Catalog ownCatalog(String actor) {
  var user=users.active(actor);
  if(!user.role().equals("ALUMNO")||user.alumnoId()==null)throw DomainException.denied();
  var linked=db.query("SELECT id,nombre FROM alumno WHERE id=?",(r,n)->new Student(r.getLong(1),r.getString(2)),user.alumnoId()).stream().findFirst().orElseThrow(DomainException::missing);
  return catalogForStudent(linked);
 }
 private Catalog catalogForStudent(Student linked) {
  long student=linked.id();
  var years=db.query("SELECT y.id,y.name,y.active_period_id,c.id,c.level,c.grade,c.section FROM school_enrollment e JOIN school_year y ON y.id=e.year_id JOIN school_classroom c ON c.id=e.classroom_id WHERE e.student_id=? ORDER BY y.id DESC",
   (r,n)->new Year(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),r.getString(7)),student);
  var periods=db.query("SELECT p.id,p.year_id,p.name,p.position FROM school_period p WHERE EXISTS (SELECT 1 FROM school_enrollment e WHERE e.student_id=? AND e.year_id=p.year_id) ORDER BY p.year_id DESC,p.position",
   (r,n)->new Period(r.getString(1),r.getInt(2),r.getString(3),r.getInt(4)),student);
  return new Catalog(linked,years,periods);
 }
 private Year year(Catalog catalog,int year) {
  return catalog.years().stream().filter(y->y.id()==year).findFirst().orElseThrow(DomainException::missing);
 }
 public Report report(String actor,long student,int yearId,String periodId,Long courseId) {
  return report(catalog(actor,student),yearId,periodId,courseId);
 }
 public Report ownReport(String actor,int yearId,String periodId,Long courseId){return report(ownCatalog(actor),yearId,periodId,courseId);}
 private Report report(Catalog catalog,int yearId,String periodId,Long courseId) {
  long student=catalog.student().id();
  var year=year(catalog,yearId);
  var period=catalog.periods().stream().filter(p->p.yearId()==yearId && p.id().equals(periodId)).findFirst()
    .orElseThrow(()->DomainException.invalid("El periodo debe pertenecer al año de matrícula."));
  var courses=db.query("SELECT c.id,c.nombre FROM school_course a JOIN curso c ON c.id=a.course_id WHERE a.classroom_id=? ORDER BY c.nombre,c.id",
    (r,n)->new Course(r.getLong(1),r.getString(2)),year.classroomId());
  if(courseId!=null && courses.stream().noneMatch(c->c.id()==courseId)) throw DomainException.invalid("Selecciona un curso del aula del estudiante.");
  var marks=db.query("SELECT m.id,c.id,c.nombre,m.competency,m.assessment,m.score,r.maximum,m.achievement,m.recorded_at FROM school_mark m JOIN school_enrollment e ON e.id=m.enrollment_id JOIN school_classroom r ON r.id=e.classroom_id JOIN school_course a ON a.id=m.assignment_id JOIN curso c ON c.id=a.course_id WHERE e.student_id=? AND e.year_id=? AND m.period_id=? AND a.classroom_id=e.classroom_id ORDER BY c.nombre,m.competency,m.recorded_at,m.id",
    (r,n)->new Mark(r.getString(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getString(8),r.getTimestamp(9).toString()),student,yearId,periodId)
    .stream().filter(m->courseId==null || m.courseId()==courseId).toList();
  return new Report(catalog.student(),year,period,courses,marks);
 }
 public List<Attendance> attendance(String actor,long student,int yearId,LocalDate from,LocalDate to) {
  year(catalog(actor,student),yearId);
  if(from==null||to==null||from.isAfter(to)||from.getYear()!=yearId||to.getYear()!=yearId)
    throw DomainException.invalid("Selecciona un rango válido dentro del año de matrícula.");
  return db.query("SELECT a.attendance_date,a.status,a.note FROM school_attendance a JOIN school_enrollment e ON e.id=a.enrollment_id WHERE e.student_id=? AND e.year_id=? AND a.attendance_date BETWEEN ? AND ? ORDER BY a.attendance_date DESC",
    (r,n)->new Attendance(r.getDate(1).toString(),r.getString(2),r.getString(3)),student,yearId,Date.valueOf(from),Date.valueOf(to));
 }
}
