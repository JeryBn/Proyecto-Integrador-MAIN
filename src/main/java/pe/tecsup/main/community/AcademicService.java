package pe.tecsup.main.community;
import java.sql.Time;
import java.time.LocalTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicService {
 private final JdbcTemplate db;private final IdentityService identities;
 public AcademicService(JdbcTemplate db,IdentityService identities){this.db=db;this.identities=identities;}
 public record Course(long id,String name,String teacher){}
 public record LinkedStudent(long id,String name){}
 public record Slot(String id,long courseId,String course,String teacher,int dayOfWeek,String startsAt,String endsAt,String classroom){}
 public record NewSlot(long courseId,Integer dayOfWeek,String startsAt,String endsAt,String classroom){}
 public record Agenda(List<Course> courses,List<Slot> timetable,List<LinkedStudent> students){}
 public Agenda agenda(String username){
  var user=identities.active(username);String scope;Object parameter;
  switch(user.role()){
   case "ALUMNO"->{scope="EXISTS(SELECT 1 FROM matricula m WHERE m.curso_id=c.id AND m.alumno_id=?)";parameter=user.alumnoId();}
   case "DOCENTE"->{scope="c.docente_id=?";parameter=user.docenteId();}
   case "FAMILIAR"->{scope="EXISTS(SELECT 1 FROM guardian_link g JOIN matricula m ON m.alumno_id=g.alumno_id WHERE m.curso_id=c.id AND g.guardian=?)";parameter=username;}
   default->throw DomainException.denied();
  }
  var courses=db.query("SELECT c.id,c.nombre,d.nombre docente FROM curso c JOIN docente d ON d.id=c.docente_id WHERE "+scope+" ORDER BY c.nombre,c.id",
   (r,n)->new Course(r.getLong(1),r.getString(2),r.getString(3)),parameter);
  var students=user.role().equals("FAMILIAR")?db.query("SELECT a.id,a.nombre FROM alumno a JOIN guardian_link g ON g.alumno_id=a.id WHERE g.guardian=? ORDER BY a.nombre,a.id",
   (r,n)->new LinkedStudent(r.getLong(1),r.getString(2)),username):List.<LinkedStudent>of();
  if(courses.isEmpty())return new Agenda(courses,List.of(),students);
  String marks=String.join(",",Collections.nCopies(courses.size(),"?"));
  var slots=db.query("SELECT t.*,c.nombre curso,d.nombre docente FROM timetable t JOIN curso c ON c.id=t.curso_id JOIN docente d ON d.id=c.docente_id WHERE t.curso_id IN ("+marks+") ORDER BY t.day_of_week,t.starts_at,t.id",
   (r,n)->new Slot(r.getString("id"),r.getLong("curso_id"),r.getString("curso"),r.getString("docente"),r.getInt("day_of_week"),
    r.getTime("starts_at").toLocalTime().toString(),r.getTime("ends_at").toLocalTime().toString(),r.getString("classroom")),courses.stream().map(Course::id).toArray());
  return new Agenda(courses,slots,students);
 }
 @Transactional public String createSlot(String actor,NewSlot input){
  identities.admin(actor);String classroom=IdentityService.text(input.classroom(),100,"aula");
  if(input.dayOfWeek()==null||input.dayOfWeek()<1||input.dayOfWeek()>7)throw DomainException.invalid("Día: 1 (lunes) a 7 (domingo).");
  LocalTime start,end;
  try{start=LocalTime.parse(input.startsAt());end=LocalTime.parse(input.endsAt());}catch(Exception e){throw DomainException.invalid("Horas ISO HH:mm o HH:mm:ss requeridas.");}
  if(!start.isBefore(end)||start.getNano()!=0||end.getNano()!=0)throw DomainException.invalid("La hora final debe ser posterior a la inicial, sin fracciones.");
  var teachers=db.queryForList("SELECT docente_id FROM curso WHERE id=?",Long.class,input.courseId());if(teachers.isEmpty())throw DomainException.missing();
  db.queryForObject("SELECT id FROM docente WHERE id=? FOR UPDATE",Long.class,teachers.getFirst());
  if(db.queryForObject("""
   SELECT COUNT(*) FROM timetable t JOIN curso c ON c.id=t.curso_id
   WHERE t.day_of_week=? AND t.starts_at<? AND t.ends_at>? AND (c.docente_id=? OR t.classroom=?)
   """,Long.class,input.dayOfWeek(),Time.valueOf(end),Time.valueOf(start),teachers.getFirst(),classroom)>0)
   throw DomainException.invalid("El horario se cruza con otra clase del docente o aula.");
  String id=UUID.randomUUID().toString();
  db.update("INSERT INTO timetable VALUES(?,?,?,?,?,?)",id,input.courseId(),input.dayOfWeek(),Time.valueOf(start),Time.valueOf(end),classroom);return id;
 }
 public void removeSlot(String actor,String id){identities.admin(actor);if(db.update("DELETE FROM timetable WHERE id=?",id)==0)throw DomainException.missing();}
}
