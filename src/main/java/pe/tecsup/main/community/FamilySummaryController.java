package pe.tecsup.main.community;

import java.security.Principal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FamilySummaryController {
    private final JdbcTemplate db;
    private final Clock clock;
    private final IdentityService identity;

    FamilySummaryController(JdbcTemplate db, Clock clock, IdentityService identity) {
        this.db = db;
        this.clock = clock;
        this.identity = identity;
    }

    record Student(long id, String name) {}
    record Attendance(long present, long absent, long late) {}
    record Task(String title, String course, String dueAt, boolean overdue) {}
    record SummaryStudent(long id, String name, Attendance attendance, List<Task> tasks) {}
    record Summary(String attendanceFrom, String attendanceTo, List<SummaryStudent> students) {}

    @GetMapping("/api/v1/family/weekly")
    @Transactional(readOnly = true)
    public Summary weekly(Principal principal) {
        var actor = identity.active(principal.getName());
        if (!"FAMILIAR".equals(actor.role())) throw DomainException.denied();
        var today = LocalDate.now(clock);
        var from = today.minusDays(6);
        var students = db.query("""
                SELECT a.id,a.nombre FROM guardian_link g JOIN alumno a ON a.id=g.alumno_id
                JOIN main_user u ON u.alumno_id=a.id AND u.role='ALUMNO' AND u.enabled=TRUE
                WHERE g.guardian=? ORDER BY a.nombre,a.id
                """, (row,index)->new Student(row.getLong(1),row.getString(2)), actor.username());
        return new Summary(from.toString(),today.toString(),
                students.stream().map(student->summary(student,from,today)).toList());
    }

    private SummaryStudent summary(Student student, LocalDate from, LocalDate today) {
        var attendance = db.queryForObject("""
                SELECT COUNT(CASE WHEN status='PRESENT' THEN 1 END),
                       COUNT(CASE WHEN status='ABSENT' THEN 1 END),
                       COUNT(CASE WHEN status='LATE' THEN 1 END)
                FROM course_attendance WHERE student_id=? AND class_date BETWEEN ? AND ?
                """,(row,index)->new Attendance(row.getLong(1),row.getLong(2),row.getLong(3)),
                student.id(),Date.valueOf(from),Date.valueOf(today));
        var now=LocalDateTime.now(clock);
        var tasks = db.query("""
                SELECT a.titulo,c.nombre,a.fecha_limite FROM matricula m JOIN actividad a ON a.curso_id=m.curso_id
                JOIN curso c ON c.id=a.curso_id
                WHERE m.alumno_id=? AND a.fecha_limite<?
                AND NOT EXISTS(SELECT 1 FROM entrega e WHERE e.actividad_id=a.id AND e.alumno_id=m.alumno_id AND e.status='SUBMITTED')
                ORDER BY CASE WHEN a.fecha_limite>=? THEN 0 ELSE 1 END,
                         CASE WHEN a.fecha_limite>=? THEN a.fecha_limite END,
                         a.fecha_limite DESC,a.id LIMIT 5
                """,(row,index)->{
                    var due=row.getTimestamp(3).toLocalDateTime();
                    return new Task(row.getString(1),row.getString(2),
                            due.atZone(clock.getZone()).toInstant().toString(),due.isBefore(now));
                },student.id(),Timestamp.valueOf(now.plusDays(7)),Timestamp.valueOf(now),Timestamp.valueOf(now));
        return new SummaryStudent(student.id(),student.name(),attendance,tasks);
    }
}
