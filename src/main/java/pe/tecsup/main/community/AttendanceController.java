package pe.tecsup.main.community;

import java.security.Principal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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
    public List<Mark> roster(Principal principal, @PathVariable long courseId, @PathVariable LocalDate date) {
        requireTeacher(principal.getName(), courseId, false);
        return db.query("""
            SELECT a.id,a.nombre,r.status FROM matricula m JOIN alumno a ON a.id=m.alumno_id
            LEFT JOIN course_attendance r ON r.course_id=m.curso_id AND r.student_id=a.id AND r.class_date=?
            WHERE m.curso_id=? ORDER BY a.nombre,a.id
            """, (rs, index) -> new Mark(rs.getLong(1), rs.getString(2), rs.getString(3)),
            Date.valueOf(date), courseId);
    }

    @PutMapping
    @Transactional
    public Mark save(Principal principal, @PathVariable long courseId, @PathVariable LocalDate date,
                     @RequestBody Input input) {
        String actor = principal.getName();
        requireTeacher(actor, courseId, true);
        if (date.isAfter(LocalDate.now(clock)) || date.getYear() < 1970 || input.status() == null) {
            throw DomainException.invalid("Selecciona una fecha válida y un estado de asistencia.");
        }
        var names = db.queryForList("""
            SELECT a.nombre FROM matricula m JOIN alumno a ON a.id=m.alumno_id
            WHERE m.curso_id=? AND a.id=? FOR UPDATE
            """, String.class, courseId, input.studentId());
        if (names.isEmpty()) throw DomainException.missing();
        var sqlDate = Date.valueOf(date);
        var now = LocalDateTime.now(clock);
        var prior = db.queryForList("""
            SELECT status FROM course_attendance WHERE course_id=? AND student_id=? AND class_date=?
            """, String.class, courseId, input.studentId(), sqlDate);
        String status = input.status().name();
        if (prior.isEmpty()) {
            db.update("""
                INSERT INTO course_attendance(course_id,student_id,class_date,status,recorded_by,recorded_at)
                VALUES(?,?,?,?,?,?)
                """, courseId, input.studentId(), sqlDate, status, actor, now);
        } else if (!status.equals(prior.getFirst())) {
            db.update("""
                UPDATE course_attendance SET status=?,recorded_by=?,recorded_at=?
                WHERE course_id=? AND student_id=? AND class_date=?
                """, status, actor, now, courseId, input.studentId(), sqlDate);
        } else {
            return new Mark(input.studentId(), names.getFirst(), status);
        }
        audit.record(actor, "ATTENDANCE_RECORDED", courseId + ":" + input.studentId() + ":" + date);
        return new Mark(input.studentId(), names.getFirst(), status);
    }

    public enum Status { PRESENT, ABSENT, LATE }
    public record Input(long studentId, Status status) {}
    public record Mark(long studentId, String name, String status) {}
}
