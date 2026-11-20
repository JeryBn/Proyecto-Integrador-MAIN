package pe.tecsup.main.community;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public List<Post> feed(Principal principal, @RequestParam(defaultValue="recent") String view,
                           @RequestParam(defaultValue="0") int offset) {
        var actor = identities.active(principal.getName());
        requireCampusMember(actor);
        IdentityService.page(offset, 20);
        if (!List.of("recent", "for-you", "mine").contains(view)) throw DomainException.invalid("Vista de campus inválida.");
        String select = """
            SELECT p.id,u.display_name,p.body,p.state,p.created_at
            FROM campus_post p JOIN main_user u ON u.username=p.author
            WHERE u.enabled=TRUE AND
            """;
        String order = " ORDER BY p.created_at DESC,p.id DESC LIMIT 20 OFFSET ?";
        var mapper = (org.springframework.jdbc.core.RowMapper<Post>) (r,n) ->
            new Post(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getTimestamp(5).toLocalDateTime().toString());
        if ("mine".equals(view)) return db.query(select + " p.author=?" + order, mapper,actor.username(),offset);
        String visible = """
             p.state='PUBLISHED' AND NOT EXISTS (
              SELECT 1 FROM contact_preference b WHERE b.blocked=TRUE
              AND ((b.owner=? AND b.subject=p.author) OR (b.subject=? AND b.owner=p.author))
             )
            """;
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
        if (!"ALUMNO".equals(actor.role())) throw DomainException.denied();
        String text=IdentityService.text(draft.text(),1000,"Publicación");
        String id = UUID.randomUUID().toString();
        var now=LocalDateTime.now(clock);
        db.update("INSERT INTO campus_post(id,author,body,state,created_at) VALUES(?,?,?,'PENDING',?)",
            id,actor.username(),text,now);
        return new Post(id,actor.name(),text,"PENDING",now.toString());
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String,Boolean> withdraw(Principal principal, @PathVariable String id) {
        var actor=identities.active(principal.getName());
        if (!"ALUMNO".equals(actor.role())) throw DomainException.denied();
        if (db.update("DELETE FROM campus_post WHERE id=? AND author=?",id,actor.username()) != 1) {
            throw DomainException.missing();
        }
        return java.util.Map.of("withdrawn",true);
    }

    private void requireCampusMember(IdentityService.Account actor) {
        if (!List.of("ALUMNO","DOCENTE").contains(actor.role())) throw DomainException.denied();
    }

    public record Draft(String text) {}
    public record Post(String id,String author,String text,String state,String createdAt) {}
}
