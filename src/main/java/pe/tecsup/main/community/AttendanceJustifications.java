package pe.tecsup.main.community;

import java.security.Principal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance/justifications")
class AttendanceJustifications {
    private final JdbcTemplate db;
    private final Clock clock;
    private final IdentityService identities;
    private final AdministrativeAudit audit;

    AttendanceJustifications(JdbcTemplate db,Clock clock,IdentityService identities,AdministrativeAudit audit){
        this.db=db;this.clock=clock;this.identities=identities;this.audit=audit;
    }

    record Item(long courseId,String course,long studentId,String student,String date,int session,
                String attendance,String id,String state,String reason,String decisionNote) {}
    record Request(long courseId,long studentId,LocalDate date,int session,String reason) {}
    record Decision(boolean approved,String note) {}
    record Result(String id,String state) {}

    private static final String SELECT="""
            SELECT a.course_id,c.nombre,a.student_id,s.nombre,a.class_date,a.session_no,a.status,
                   j.id,j.state,j.reason,j.decision_note
            FROM course_attendance a JOIN alumno s ON s.id=a.student_id JOIN curso c ON c.id=a.course_id
            LEFT JOIN attendance_justification j ON j.course_id=a.course_id AND j.student_id=a.student_id
                 AND j.class_date=a.class_date AND j.session_no=a.session_no
            """;
    private static final RowMapper<Item> ITEM=(r,n)->new Item(r.getLong(1),r.getString(2),r.getLong(3),
            r.getString(4),r.getDate(5).toString(),r.getInt(6),r.getString(7),r.getString(8),
            r.getString(9),r.getString(10),r.getString(11));

    @GetMapping
    public List<Item> list(Principal principal,@RequestParam(required=false) Long courseId,
                           @RequestParam(defaultValue="0") int offset) {
        var actor=identities.active(principal.getName());
        IdentityService.page(offset,30);
        String order=" ORDER BY a.class_date DESC,a.session_no DESC,a.course_id,a.student_id LIMIT 30 OFFSET ?";
        var since=Date.valueOf(LocalDate.now(clock).minusDays(30));
        return switch(actor.role()){
            case "DOCENTE" -> {
                if(courseId==null)throw DomainException.invalid("Selecciona un curso.");
                requireTeacher(actor,courseId,false);
                yield db.query(SELECT+" WHERE a.course_id=? AND j.id IS NOT NULL"+order,ITEM,courseId,offset);
            }
            case "ALUMNO" -> db.query(SELECT+" WHERE a.student_id=? AND a.class_date>=?"+
                    " AND (a.status IN ('ABSENT','LATE') OR j.id IS NOT NULL)"+order,ITEM,actor.alumnoId(),since,offset);
            case "FAMILIAR" -> db.query(SELECT+"""
                     WHERE a.class_date>=? AND (a.status IN ('ABSENT','LATE') OR j.id IS NOT NULL)
                     AND EXISTS(SELECT 1 FROM guardian_link g WHERE g.alumno_id=a.student_id AND g.guardian=?)
                    """+order,ITEM,since,actor.username(),offset);
            default -> throw DomainException.denied();
        };
    }

    @PostMapping
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Result submit(Principal principal,@RequestBody Request input) {
        var actor=identities.active(principal.getName());
        if(!maySubmit(actor,input.studentId()))throw DomainException.missing();
        if(input.date()==null||input.date().isAfter(LocalDate.now(clock))||
                input.date().isBefore(LocalDate.now(clock).minusDays(30))||input.session()<1||input.session()>8)
            throw DomainException.invalid("Selecciona una sesión de los últimos 30 días.");
        String reason=IdentityService.text(input.reason(),500,"motivo");
        if(db.queryForList("SELECT id FROM curso WHERE id=? FOR UPDATE",Long.class,input.courseId()).isEmpty())
            throw DomainException.missing();
        if(!maySubmit(actor,input.studentId()))throw DomainException.missing();
        var date=Date.valueOf(input.date());
        var matches=db.query(SELECT+" WHERE a.course_id=? AND a.student_id=? AND a.class_date=? AND a.session_no=?",
                ITEM,input.courseId(),input.studentId(),date,input.session());
        if(matches.isEmpty())throw DomainException.missing();
        var mark=matches.getFirst();
        if(mark.id()!=null){
            if(reason.equals(mark.reason()))return new Result(mark.id(),mark.state());
            throw new DomainException(HttpStatus.CONFLICT,"JUSTIFICATION_EXISTS","Ya existe una solicitud para esta sesión.");
        }
        if(!List.of("ABSENT","LATE").contains(mark.attendance()))throw DomainException.missing();
        String id=UUID.randomUUID().toString();
        db.update("""
                INSERT INTO attendance_justification
                (id,course_id,student_id,class_date,session_no,requested_by,reason,state,created_at)
                VALUES(?,?,?,?,?,?,?,'PENDING',?)
                """,id,input.courseId(),input.studentId(),date,input.session(),actor.username(),reason,now());
        audit.record(actor.username(),"JUSTIFICATION_REQUESTED",id);
        return new Result(id,"PENDING");
    }

    @PutMapping("/{id}")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Result decide(Principal principal,@PathVariable String id,@RequestBody Decision input){
        var actor=identities.active(principal.getName());
        if(!"DOCENTE".equals(actor.role()))throw DomainException.denied();
        var matches=db.queryForList("SELECT course_id FROM attendance_justification WHERE id=?",Long.class,id);
        if(matches.isEmpty())throw DomainException.missing();
        requireTeacher(actor,matches.getFirst(),true);
        String note=input.note()==null||input.note().isBlank()?null:IdentityService.text(input.note(),300,"decisión");
        String state=input.approved()?"APPROVED":"REJECTED";
        int changed=db.update("""
                UPDATE attendance_justification SET state=?,decision_note=?,reviewed_at=?
                WHERE id=? AND state='PENDING'
                """,state,note,now(),id);
        if(changed==0){
            var prior=db.queryForMap("SELECT state,decision_note FROM attendance_justification WHERE id=?",id);
            if(state.equals(prior.get("state"))&&Objects.equals(note,prior.get("decision_note")))return new Result(id,state);
            throw new DomainException(HttpStatus.CONFLICT,"JUSTIFICATION_DECIDED","La solicitud ya tiene una decisión.");
        }
        audit.record(actor.username(),"JUSTIFICATION_"+state,id);
        return new Result(id,state);
    }

    private boolean maySubmit(IdentityService.Account actor,long studentId){
        return "ALUMNO".equals(actor.role())&&Objects.equals(actor.alumnoId(),studentId)||
                "FAMILIAR".equals(actor.role())&&db.queryForObject(
                        "SELECT COUNT(*) FROM guardian_link WHERE guardian=? AND alumno_id=?",Long.class,actor.username(),studentId)>0;
    }

    private void requireTeacher(IdentityService.Account actor,long courseId,boolean lock){
        if(!"DOCENTE".equals(actor.role()))throw DomainException.denied();
        var owners=db.queryForList("SELECT docente_id FROM curso WHERE id=?"+(lock?" FOR UPDATE":""),Long.class,courseId);
        if(owners.isEmpty()||!Objects.equals(actor.docenteId(),owners.getFirst()))throw DomainException.missing();
    }

    private Timestamp now(){return Timestamp.valueOf(LocalDateTime.now(clock));}
}
