package pe.tecsup.main.community;

import java.security.Principal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching/courses/{courseId}/attendance/{date}")
public class AttendanceController {
    private final JdbcTemplate db;
    private final IdentityService identities;
    private final AdministrativeAudit audit;
    private final Clock clock;

    public AttendanceController(JdbcTemplate db, IdentityService identities, AdministrativeAudit audit, Clock clock) {
        this.db = db;
        this.identities = identities;
        this.audit = audit;
        this.clock = clock;
    }

    private void requireTeacher(String actor, long courseId, boolean lock) {
        var account = identities.active(actor);
        if (!"DOCENTE".equals(account.role())) throw DomainException.denied();
        var owners = db.queryForList("SELECT docente_id FROM curso WHERE id=?" +
                (lock ? " FOR UPDATE" : ""), Long.class, courseId);
        if (owners.isEmpty() || !Objects.equals(account.docenteId(), owners.getFirst())) {
            throw DomainException.missing();
        }
    }

    @GetMapping
    public List<Mark> roster(Principal principal, @PathVariable long courseId, @PathVariable LocalDate date,
                             @RequestParam(defaultValue="1") int session) {
        requireTeacher(principal.getName(), courseId, false);
        validateSession(date,session);
        return marks(courseId,date,session);
    }

    private List<Mark> marks(long courseId, LocalDate date, int session) {
        return db.query("""
            SELECT a.id,a.nombre,r.status FROM matricula m JOIN alumno a ON a.id=m.alumno_id
            LEFT JOIN course_attendance r ON r.course_id=m.curso_id AND r.student_id=a.id AND r.class_date=? AND r.session_no=?
            WHERE m.curso_id=? ORDER BY a.nombre,a.id
            """, (rs, index) -> new Mark(rs.getLong(1), rs.getString(2), rs.getString(3)),
            Date.valueOf(date), session, courseId);
    }

    @PutMapping
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Mark save(Principal principal, @PathVariable long courseId, @PathVariable LocalDate date,
                     @RequestParam(defaultValue="1") int session, @RequestBody Input input) {
        String actor = principal.getName();
        requireTeacher(actor, courseId, true);
        validateSession(date,session);
        if(input.status()==null)throw DomainException.invalid("Selecciona un estado de asistencia.");
        return saveMark(actor,courseId,date,session,input);
    }

    @PutMapping("/batch")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public List<Mark> saveBatch(Principal principal, @PathVariable long courseId, @PathVariable LocalDate date,
                                @RequestParam(defaultValue="1") int session, @RequestBody Batch batch) {
        String actor=principal.getName();
        requireTeacher(actor,courseId,true);
        validateSession(date,session);
        if(batch.changes()==null||batch.changes().isEmpty()||batch.changes().size()>300)
            throw DomainException.invalid("Envía entre 1 y 300 registros de asistencia.");
        var ids=new HashSet<Long>();
        var roster=new java.util.HashMap<Long,String>();
        for(Mark mark:marks(courseId,date,session))roster.put(mark.studentId(),mark.status());
        for(Change change:batch.changes()){
            if(change==null||change.status()==null||!ids.add(change.studentId()))
                throw DomainException.invalid("Revisa los estados y estudiantes duplicados.");
            if(!roster.containsKey(change.studentId()))throw DomainException.missing();
            String current=roster.get(change.studentId());
            String expected=change.expectedStatus()==null?null:change.expectedStatus().name();
            if(!Objects.equals(current,expected)&&!Objects.equals(current,change.status().name()))
                throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"ATTENDANCE_CONFLICT",
                    "Otra sesión modificó la asistencia. Actualiza la lista antes de guardar.");
        }
        for(Change change:batch.changes())saveMark(actor,courseId,date,session,new Input(change.studentId(),change.status()));
        return marks(courseId,date,session);
    }

    private void validateSession(LocalDate date,int session){
        if(session<1||session>8)throw DomainException.invalid("Selecciona una sesión entre 1 y 8.");
        if(date.isAfter(LocalDate.now(clock))||date.getYear()<1970)
            throw DomainException.invalid("Selecciona una fecha válida.");
    }

    private Mark saveMark(String actor,long courseId,LocalDate date,int session,Input input){
        var names = db.queryForList("""
            SELECT a.nombre FROM matricula m JOIN alumno a ON a.id=m.alumno_id
            WHERE m.curso_id=? AND a.id=? FOR UPDATE
            """, String.class, courseId, input.studentId());
        if (names.isEmpty()) throw DomainException.missing();
        var sqlDate = Date.valueOf(date);
        var now = LocalDateTime.now(clock);
        var prior = db.queryForList("""
            SELECT status FROM course_attendance WHERE course_id=? AND student_id=? AND class_date=? AND session_no=?
            """, String.class, courseId, input.studentId(), sqlDate, session);
        String status = input.status().name();
        if (prior.isEmpty()) {
            db.update("""
                INSERT INTO course_attendance(course_id,student_id,class_date,session_no,status,recorded_by,recorded_at)
                VALUES(?,?,?,?,?,?,?)
                """, courseId, input.studentId(), sqlDate, session, status, actor, now);
        } else if (!status.equals(prior.getFirst())) {
            db.update("""
                UPDATE course_attendance SET status=?,recorded_by=?,recorded_at=?
                WHERE course_id=? AND student_id=? AND class_date=? AND session_no=?
                """, status, actor, now, courseId, input.studentId(), sqlDate, session);
        } else {
            return new Mark(input.studentId(), names.getFirst(), status);
        }
        audit.record(actor, "ATTENDANCE_RECORDED", courseId + ":" + input.studentId() + ":" + date + ":" + session);
        return new Mark(input.studentId(), names.getFirst(), status);
    }

    public enum Status { PRESENT, ABSENT, LATE }
    public record Batch(List<Change> changes) {}
    public record Change(long studentId,Status status,Status expectedStatus) {}
    public record Input(long studentId, Status status) {}
    public record Mark(long studentId, String name, String status) {}
}
