package pe.tecsup.main.community;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/campus/feed")
public class CampusFeedController {
    private final JdbcTemplate db;
    private final IdentityService identities;
    private final Clock clock;

    public CampusFeedController(JdbcTemplate db, IdentityService identities, Clock clock) {
        this.db = db;
        this.identities = identities;
        this.clock = clock;
    }

    private static final String POST_SELECT="""
            SELECT p.id,u.display_name,p.body,p.state,p.created_at,c.nombre,p.pinned,p.post_kind,p.author
            FROM campus_post p JOIN main_user u ON u.username=p.author
            LEFT JOIN curso c ON c.id=p.course_id
            """;
    private static final org.springframework.jdbc.core.RowMapper<Post> POST=(r,n)->
            new Post(r.getString(1),r.getString(2),r.getString(3),r.getString(4),
                    r.getTimestamp(5).toLocalDateTime().toString(),r.getString(6),r.getBoolean(7),r.getString(8));

    @GetMapping
    public List<Post> feed(Principal principal, @RequestParam(defaultValue="recent") String view,
                           @RequestParam(defaultValue="0") int offset, @RequestParam(required=false) Long courseId) {
        var actor = identities.active(principal.getName());
        requireCampusMember(actor);
        IdentityService.page(offset, 20);
        if(courseId!=null)requireCourse(actor,courseId);
        if (!List.of("recent", "for-you", "mine").contains(view)) throw DomainException.invalid("Vista de campus inválida.");
        String select = POST_SELECT + " WHERE u.enabled=TRUE AND p.withdrawn=FALSE AND p.parent_id IS NULL AND ";
        String order = " ORDER BY p.created_at DESC,p.id DESC LIMIT 20 OFFSET ?";
        var mapper=POST;
        if ("mine".equals(view)) {
            return courseId==null
                ? db.query(select+" p.author=? AND p.course_id IS NULL"+order,mapper,actor.username(),offset)
                : db.query(select+" p.author=? AND p.course_id=?"+order,mapper,actor.username(),courseId,offset);
        }
        String visible = """
             p.state='PUBLISHED' AND NOT EXISTS (
              SELECT 1 FROM contact_preference b WHERE b.blocked=TRUE
              AND ((b.owner=? AND b.subject=p.author) OR (b.subject=? AND b.owner=p.author))
             )
            """;
        if(courseId!=null)
            return db.query(select+visible+" AND p.course_id=? ORDER BY p.pinned DESC,p.created_at DESC,p.id DESC LIMIT 20 OFFSET ?",mapper,actor.username(),actor.username(),courseId,offset);
        visible+=" AND p.course_id IS NULL";
        if ("for-you".equals(view) && actor.alumnoId() != null) {
            // Classroom relevance uses enrollment only and does not use chat contents.
            order = """
                 ORDER BY CASE WHEN EXISTS (
                   SELECT 1 FROM matricula mine JOIN matricula peers ON mine.curso_id=peers.curso_id
                   WHERE mine.alumno_id=? AND peers.alumno_id=u.alumno_id
                 ) THEN 0 ELSE 1 END,p.created_at DESC,p.id DESC LIMIT 20 OFFSET ?
                """;
            return db.query(select+visible+order,mapper,actor.username(),actor.username(),actor.alumnoId(),offset);
        }
        return db.query(select+visible+order,mapper,actor.username(),actor.username(),offset);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post publish(Principal principal, @RequestBody Draft draft) {
        var actor=identities.active(principal.getName());
        requireCampusMember(actor);
        String course = draft.courseId()==null ? null : requireCourse(actor,draft.courseId());
        if (course==null&&!"ALUMNO".equals(actor.role())) throw DomainException.denied();
        if(draft.pinned()&&(course==null||!"DOCENTE".equals(actor.role())))throw DomainException.denied();
        if(draft.question()&&(course==null||draft.pinned()))
            throw DomainException.invalid("Las preguntas se publican dentro de un curso.");
        return store(actor,draft.text(),draft.courseId(),course,draft.pinned(),draft.question()?"QUESTION":"POST",null);
    }

    record Reply(String text) {}
    record Thread(long courseId,String author) {}

    @GetMapping("/{id}/replies")
    public List<Post> replies(Principal principal,@PathVariable String id,@RequestParam(defaultValue="0") int offset){
        var actor=identities.active(principal.getName());
        requireCampusMember(actor);
        IdentityService.page(offset,20);
        thread(actor,id,false);
        return db.query(POST_SELECT+"""
            WHERE p.parent_id=? AND p.withdrawn=FALSE AND u.enabled=TRUE
              AND (p.state='PUBLISHED' OR p.author=?)
              AND NOT EXISTS(SELECT 1 FROM contact_preference b WHERE b.blocked=TRUE
                  AND ((b.owner=? AND b.subject=p.author) OR (b.subject=? AND b.owner=p.author)))
            ORDER BY p.created_at,p.id LIMIT 20 OFFSET ?
            """,POST,id,actor.username(),actor.username(),actor.username(),offset);
    }

    @PostMapping("/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Post reply(Principal principal,@PathVariable String id,@RequestBody Reply input){
        var actor=identities.active(principal.getName());
        requireCampusMember(actor);
        var thread=thread(actor,id,true);
        return store(actor,input.text(),thread.courseId(),null,false,"REPLY",id);
    }

    private Thread thread(IdentityService.Account actor,String id,boolean lock){
        var items=db.query("""
            SELECT p.course_id,p.author FROM campus_post p JOIN main_user u ON u.username=p.author
            WHERE p.id=? AND p.post_kind='QUESTION' AND p.state='PUBLISHED' AND p.withdrawn=FALSE AND u.enabled=TRUE
            """+(lock?" FOR UPDATE":""),(r,n)->new Thread(r.getLong(1),r.getString(2)),id);
        if(items.isEmpty())throw DomainException.missing();
        var thread=items.getFirst();
        requireCourse(actor,thread.courseId());
        if(identities.blocked(actor.username(),thread.author()))throw DomainException.missing();
        return thread;
    }

    private Post store(IdentityService.Account actor,String body,Long courseId,String course,boolean pinned,String kind,String parent){
        String text=IdentityService.text(body,1000,"Publicación");
        String id = UUID.randomUUID().toString();
        var now=LocalDateTime.now(clock);
        db.update("""
            INSERT INTO campus_post(id,author,body,state,created_at,course_id,pinned,post_kind,parent_id)
            VALUES(?,?,?,'PENDING',?,?,?,?,?)
            """,id,actor.username(),text,now,courseId,pinned,kind,parent);
        return new Post(id,actor.name(),text,"PENDING",now.toString(),course,pinned,kind);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public java.util.Map<String,Boolean> withdraw(Principal principal, @PathVariable String id) {
        var actor=identities.active(principal.getName());
        requireCampusMember(actor);
        if (db.update("UPDATE campus_post SET withdrawn=TRUE WHERE id=? AND author=? AND withdrawn=FALSE",id,actor.username()) != 1) {
            throw DomainException.missing();
        }
        db.update("UPDATE campus_post SET withdrawn=TRUE WHERE parent_id=?",id);
        return java.util.Map.of("withdrawn",true);
    }

    private void requireCampusMember(IdentityService.Account actor) {
        if (!List.of("ALUMNO","DOCENTE").contains(actor.role())) throw DomainException.denied();
    }

    private String requireCourse(IdentityService.Account actor,long courseId){
        var names = "DOCENTE".equals(actor.role())
            ? db.queryForList("SELECT nombre FROM curso WHERE id=? AND docente_id=?",String.class,courseId,actor.docenteId())
            : db.queryForList("""
                SELECT c.nombre FROM curso c JOIN matricula m ON m.curso_id=c.id
                WHERE c.id=? AND m.alumno_id=?
                """,String.class,courseId,actor.alumnoId());
        if(names.isEmpty())throw DomainException.missing();
        return names.getFirst();
    }

    public record Draft(String text,Long courseId,boolean pinned,boolean question) {
        public Draft(String text,Long courseId,boolean pinned){this(text,courseId,pinned,false);}
    }
    public record Post(String id,String author,String text,String state,String createdAt,String course,boolean pinned,String kind) {}
}
