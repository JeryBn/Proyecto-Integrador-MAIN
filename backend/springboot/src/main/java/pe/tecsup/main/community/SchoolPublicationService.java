package pe.tecsup.main.community;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolPublicationService {
 private final JdbcTemplate db; private final IdentityService users; private final FamilySchoolService family;
 private final AdministrativeAudit audit; private final Clock clock;
 public SchoolPublicationService(JdbcTemplate db,IdentityService users,FamilySchoolService family,AdministrativeAudit audit,Clock clock){this.db=db;this.users=users;this.family=family;this.audit=audit;this.clock=clock;}
 public record Scope(long courseId,String course,String classroomId,String classroom) {}
 public record Reader(String name,String readAt) {}
 public record Publication(String id,long courseId,String course,String classroomId,String kind,String title,String instructions,String dueAt,String createdAt,boolean read,int audience,List<Reader> readers) {}
 public record Input(String requestId,long courseId,String classroomId,String kind,String title,String instructions,String dueAt) {}
 private void teacher(String actor){if(!users.active(actor).role().equals("DOCENTE"))throw DomainException.denied();}
 public List<Scope> scopes(String actor) {
  teacher(actor);var user=users.active(actor);
  var result=new ArrayList<>(db.query("SELECT id,nombre FROM curso WHERE docente_id=? ORDER BY nombre,id",
    (r,n)->new Scope(r.getLong(1),r.getString(2),null,"Todos los matriculados del curso"),user.docenteId()));
  result.addAll(db.query("SELECT c.id,c.nombre,r.id,r.year_id,r.level,r.grade,r.section FROM school_course s JOIN curso c ON c.id=s.course_id JOIN school_classroom r ON r.id=s.classroom_id WHERE s.teacher_username=? ORDER BY r.year_id DESC,c.nombre,r.grade,r.section",
    (r,n)->new Scope(r.getLong(1),r.getString(2),r.getString(3),r.getInt(4)+" · "+r.getString(5)+" · "+r.getInt(6)+"° "+r.getString(7)),actor));
  return result;
 }
 private List<Publication> load(String sql,Object...args) {
  return db.query(sql,(r,n)->new Publication(r.getString("id"),r.getLong("course_id"),r.getString("course"),r.getString("classroom_id"),r.getString("kind"),r.getString("title"),r.getString("instructions"),r.getTimestamp("due_at")==null?null:r.getTimestamp("due_at").toString(),r.getTimestamp("created_at").toString(),false,0,List.of()),args);
 }
 private static final String SELECT="SELECT p.*,c.nombre AS course FROM school_publication p JOIN curso c ON c.id=p.course_id ";
 private List<String> audience(Publication p) {
  return db.queryForList("SELECT DISTINCT g.guardian FROM guardian_link g JOIN main_user u ON u.username=g.guardian JOIN matricula m ON m.alumno_id=g.alumno_id WHERE m.curso_id=? AND u.enabled=TRUE AND u.role='FAMILIAR' AND (? IS NULL OR EXISTS(SELECT 1 FROM school_enrollment e WHERE e.student_id=g.alumno_id AND e.classroom_id=?))",
    String.class,p.courseId(),p.classroomId(),p.classroomId());
 }
 public List<Publication> teaching(String actor) {
  teacher(actor);
  return load(SELECT+"WHERE p.teacher_username=? ORDER BY p.created_at DESC,p.id",actor).stream().map(p->{
   var authorized=audience(p);
   var readers=db.query("SELECT u.display_name,r.read_at,r.guardian FROM school_publication_read r JOIN main_user u ON u.username=r.guardian WHERE r.publication_id=? ORDER BY u.display_name",
    (r,n)->authorized.contains(r.getString(3))?new Reader(r.getString(1),r.getTimestamp(2).toString()):null,p.id()).stream().filter(Objects::nonNull).toList();
   return new Publication(p.id(),p.courseId(),p.course(),p.classroomId(),p.kind(),p.title(),p.instructions(),p.dueAt(),p.createdAt(),false,authorized.size(),readers);
  }).toList();
 }
 private List<Publication> forStudent(long student) {
  return load(SELECT+"WHERE EXISTS(SELECT 1 FROM matricula m WHERE m.alumno_id=? AND m.curso_id=p.course_id) AND (p.classroom_id IS NULL OR EXISTS(SELECT 1 FROM school_enrollment e WHERE e.student_id=? AND e.classroom_id=p.classroom_id)) ORDER BY p.created_at DESC,p.id",student,student);
 }
 public List<Publication> family(String actor,long student) {
  family.requireStudent(actor,student);
  var read=new HashSet<>(db.queryForList("SELECT publication_id FROM school_publication_read WHERE guardian=?",String.class,actor));
  return forStudent(student).stream().map(p->new Publication(p.id(),p.courseId(),p.course(),p.classroomId(),p.kind(),p.title(),p.instructions(),p.dueAt(),p.createdAt(),read.contains(p.id()),0,List.of())).toList();
 }
 public List<Publication> own(String actor) {
  var user=users.active(actor);if(!user.role().equals("ALUMNO"))throw DomainException.denied();
  return forStudent(user.alumnoId());
 }
 @Transactional public void read(String actor,long student,String id) {
  // A revoked relationship or another student's publication cannot be marked.
  if(family(actor,student).stream().noneMatch(p->p.id().equals(id)))throw DomainException.denied();
  db.queryForObject("SELECT id FROM school_publication WHERE id=? FOR UPDATE",String.class,id);
  if(db.queryForObject("SELECT COUNT(*) FROM school_publication_read WHERE publication_id=? AND guardian=?",Long.class,id,actor)==0)
   db.update("INSERT INTO school_publication_read VALUES(?,?,?)",id,actor,Timestamp.valueOf(LocalDateTime.now(clock)));
 }
 @Transactional public Map<String,String> publish(String actor,Input input) {
  var scope=scopes(actor).stream().filter(s->s.courseId()==input.courseId() && Objects.equals(s.classroomId(),input.classroomId())).findFirst().orElseThrow(DomainException::denied);
  String id;try{id=UUID.fromString(input.requestId()).toString();}catch(Exception e){throw DomainException.invalid("Identificador de publicación inválido.");}
  if(input.kind()==null || !Set.of("NOTICE","TASK","EXAM").contains(input.kind()))throw DomainException.invalid("Selecciona comunicado, tarea o examen.");
  String title=IdentityService.text(input.title(),160,"título"),instructions=IdentityService.text(input.instructions(),2000,"instrucciones");
  Timestamp due=null;
  if(input.dueAt()!=null&&!input.dueAt().isBlank()){
   try{var local=LocalDateTime.parse(input.dueAt());if(local.getYear()<1000||local.getYear()>9999||local.getNano()!=0)throw new IllegalArgumentException();due=Timestamp.valueOf(local);}catch(Exception e){throw DomainException.invalid("La fecha debe tener año de cuatro cifras y hora, sin fracciones de segundo.");}
  }
  if(!input.kind().equals("NOTICE")&&due==null)throw DomainException.invalid("La fecha de entrega o del examen es obligatoria.");
  var previous=load(SELECT+"WHERE p.id=?",id);
  if(!previous.isEmpty()) {
   var old=previous.getFirst();
   boolean owner=db.queryForObject("SELECT COUNT(*) FROM school_publication WHERE id=? AND teacher_username=?",Long.class,id,actor)==1;
   if(owner&&old.courseId()==scope.courseId()&&Objects.equals(old.classroomId(),scope.classroomId())&&old.kind().equals(input.kind())&&old.title().equals(title)&&old.instructions().equals(instructions)&&Objects.equals(old.dueAt(),due==null?null:due.toString()))return Map.of("id",id);
   throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"CONFLICT","El envío ya existe con otros datos. Actualiza antes de publicar.");
  }
  db.update("INSERT INTO school_publication VALUES(?,?,?,?,?,?,?,?,?)",id,scope.courseId(),scope.classroomId(),actor,input.kind(),title,instructions,due,Timestamp.valueOf(LocalDateTime.now(clock)));
  audit.record(actor,"SCHOOL_PUBLICATION_CREATED",id);return Map.of("id",id);
 }
}
