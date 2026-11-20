package pe.tecsup.main.community;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class ReportCards {
    private final JdbcTemplate db;
    private final IdentityService identities;
    private final AdministrativeAudit audit;
    private final Clock clock;

    ReportCards(JdbcTemplate db,IdentityService identities,AdministrativeAudit audit,Clock clock){
        this.db=db;this.identities=identities;this.audit=audit;this.clock=clock;
    }

    record Period(int number,String name,String endsOn,boolean closed,boolean canClose) {}
    record Target(int year,LocalDate endsOn) {}
    record Score(long studentId,BigDecimal score) {}
    record Closure(BigDecimal maximum,List<Score> scores) {}
    record Result(boolean closed,int students) {}
    record Report(long courseId,String course,long studentId,String student,int year,int period,
                  String periodName,BigDecimal score,BigDecimal maximum) {}

    @GetMapping("/teaching/courses/{courseId}/periods")
    public List<Period> periods(Principal principal,@PathVariable long courseId){
        requireTeacher(principal.getName(),courseId,false);
        LocalDate today=LocalDate.now(clock);
        return db.query("""
                SELECT p.period_no,p.name,p.ends_on,c.closed_at
                FROM course_section a JOIN school_section s ON s.id=a.section_id
                JOIN academic_period p ON p.year_number=s.year_number
                LEFT JOIN course_period_close c ON c.course_id=a.course_id
                   AND c.year_number=p.year_number AND c.period_no=p.period_no
                WHERE a.course_id=? ORDER BY p.period_no
                """,(r,n)->{
                    var end=r.getDate(3).toLocalDate();
                    boolean closed=r.getTimestamp(4)!=null;
                    return new Period(r.getInt(1),r.getString(2),end.toString(),closed,!closed&&!today.isBefore(end));
                },courseId);
    }

    @PutMapping("/teaching/courses/{courseId}/periods/{period}/close")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Result close(Principal principal,@PathVariable long courseId,@PathVariable int period,@RequestBody Closure input){
        String actor=principal.getName();
        requireTeacher(actor,courseId,true);
        if(!validScale(input.maximum())||input.scores()==null||input.scores().isEmpty()||input.scores().size()>300)
            throw DomainException.invalid("Indica una escala válida y entre 1 y 300 notas.");
        var scores=new HashMap<Long,BigDecimal>();
        for(var row:input.scores()){
            if(row==null||row.score()==null||row.score().signum()<0||row.score().compareTo(input.maximum())>0||
                    row.score().stripTrailingZeros().scale()>2||scores.putIfAbsent(row.studentId(),row.score())!=null)
                throw DomainException.invalid("Revisa las notas y estudiantes duplicados.");
        }
        var target=db.query("""
                SELECT s.year_number,p.ends_on FROM course_section a JOIN school_section s ON s.id=a.section_id
                JOIN academic_period p ON p.year_number=s.year_number
                WHERE a.course_id=? AND p.period_no=?
                """,(r,n)->new Target(r.getInt(1),r.getDate(2).toLocalDate()),courseId,period);
        if(target.isEmpty())throw DomainException.missing();
        var academicPeriod=target.getFirst();
        int year=academicPeriod.year();
        var existing=db.queryForList("""
                SELECT maximum FROM course_period_close WHERE course_id=? AND year_number=? AND period_no=?
                """,BigDecimal.class,courseId,year,period);
        if(!existing.isEmpty()){
            var recorded=db.query("""
                    SELECT student_id,score FROM course_period_grade WHERE course_id=? AND year_number=? AND period_no=?
                    """,(r,n)->new Score(r.getLong(1),r.getBigDecimal(2)),courseId,year,period);
            if(existing.getFirst().compareTo(input.maximum())==0&&recorded.size()==scores.size()&&
                    recorded.stream().allMatch(row->scores.containsKey(row.studentId())&&row.score().compareTo(scores.get(row.studentId()))==0))
                return new Result(true,scores.size());
            throw new DomainException(HttpStatus.CONFLICT,"PERIOD_CLOSED","El período ya está cerrado.");
        }
        if(LocalDate.now(clock).isBefore(academicPeriod.endsOn()))
            throw DomainException.invalid("El período todavía está en curso.");
        var roster=db.queryForList("SELECT alumno_id FROM matricula WHERE curso_id=? ORDER BY alumno_id",Long.class,courseId);
        if(roster.size()!=scores.size()||!scores.keySet().containsAll(roster))
            throw new DomainException(HttpStatus.CONFLICT,"ROSTER_CHANGED","La matrícula cambió. Actualiza la lista antes de cerrar el período.");
        db.update("INSERT INTO course_period_close VALUES(?,?,?,?,?,?)",courseId,year,period,input.maximum(),actor,
                Timestamp.valueOf(LocalDateTime.now(clock)));
        db.batchUpdate("INSERT INTO course_period_grade VALUES(?,?,?,?,?)",
                roster.stream().map(id->new Object[]{courseId,year,period,id,scores.get(id)}).toList());
        audit.record(actor,"COURSE_PERIOD_CLOSED",courseId+":"+year+":"+period);
        return new Result(true,scores.size());
    }

    @GetMapping("/report-cards")
    public List<Report> reports(Principal principal,@RequestParam(defaultValue="0") int offset){
        var actor=identities.active(principal.getName());
        IdentityService.page(offset,30);
        String scope;
        Object key;
        switch(actor.role()){
            case "ALUMNO" -> {scope="g.student_id=?";key=actor.alumnoId();}
            case "FAMILIAR" -> {scope="EXISTS(SELECT 1 FROM guardian_link l WHERE l.guardian=? AND l.alumno_id=g.student_id)";key=actor.username();}
            default -> throw DomainException.denied();
        }
        return db.query("""
                SELECT g.course_id,c.nombre,g.student_id,a.nombre,g.year_number,g.period_no,p.name,g.score,r.maximum
                FROM course_period_grade g JOIN course_period_close r ON r.course_id=g.course_id
                  AND r.year_number=g.year_number AND r.period_no=g.period_no
                JOIN curso c ON c.id=g.course_id JOIN alumno a ON a.id=g.student_id
                JOIN academic_period p ON p.year_number=g.year_number AND p.period_no=g.period_no
                WHERE """+" "+scope+" ORDER BY g.year_number DESC,g.period_no DESC,a.nombre,c.nombre,g.course_id,g.student_id LIMIT 30 OFFSET ?",
                (r,n)->new Report(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getInt(5),r.getInt(6),
                        r.getString(7),r.getBigDecimal(8),r.getBigDecimal(9)),key,offset);
    }

    private boolean validScale(BigDecimal maximum){
        return maximum!=null&&maximum.signum()>0&&maximum.compareTo(new BigDecimal("1000"))<=0&&maximum.stripTrailingZeros().scale()<=2;
    }

    private void requireTeacher(String username,long courseId,boolean lock){
        var actor=identities.active(username);
        if(!"DOCENTE".equals(actor.role()))throw DomainException.denied();
        var owners=db.queryForList("SELECT docente_id FROM curso WHERE id=?"+(lock?" FOR UPDATE":""),Long.class,courseId);
        if(owners.isEmpty()||!Objects.equals(actor.docenteId(),owners.getFirst()))throw DomainException.missing();
    }
}
