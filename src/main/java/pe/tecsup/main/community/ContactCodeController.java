package pe.tecsup.main.community;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact-codes")
public class ContactCodeController {
    private final JdbcTemplate db;
    private final IdentityService identities;
    private final AdministrativeAudit audit;
    private final Clock clock;
    private final NotificationService notifications;
    private final SecureRandom random = new SecureRandom();

    public ContactCodeController(JdbcTemplate db, IdentityService identities, AdministrativeAudit audit, Clock clock,
                                 NotificationService notifications) {
        this.db = db;
        this.identities = identities;
        this.audit = audit;
        this.clock = clock;
        this.notifications = notifications;
    }

    @PostMapping("/mine")
    @Transactional
    public PersonalCode mine(Principal principal) {
        String actor = principal.getName();
        identities.active(actor);
        lock(actor);
        var existing = db.queryForList("SELECT code FROM contact_code WHERE username=?", String.class, actor);
        if (!existing.isEmpty()) return personalCode(existing.getFirst());
        for (int attempt = 0; attempt < 30; attempt++) {
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            try {
                db.update("INSERT INTO contact_code(username,code) VALUES(?,?)", actor, code);
                return personalCode(code);
            } catch (DuplicateKeyException collision) {
                // The unique index also protects against simultaneous allocations.
            }
        }
        throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "CONTACT_CODE_BUSY", "No se pudo asignar un código. Intenta más tarde.");
    }

    private PersonalCode personalCode(String code) {
        com.google.zxing.common.BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 0, 0);
        } catch (com.google.zxing.WriterException error) {
            throw new IllegalStateException("No se pudo generar el QR del código institucional.", error);
        }
        var rows = new java.util.ArrayList<String>(matrix.getHeight());
        for (int y = 0; y < matrix.getHeight(); y++) {
            var row = new StringBuilder(matrix.getWidth());
            for (int x = 0; x < matrix.getWidth(); x++) row.append(matrix.get(x,y) ? '1' : '0');
            rows.add(row.toString());
        }
        return new PersonalCode(code, rows);
    }

    @PostMapping("/requests")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String,String> send(Principal principal, @RequestBody Code input) {
        String actor = principal.getName();
        identities.active(actor);
        if (input.code() == null || !input.code().matches("[0-9]{6}")) {
            throw DomainException.invalid("Escribe un código de seis dígitos.");
        }
        var targets = db.queryForList("SELECT username FROM contact_code WHERE code=?", String.class, input.code());
        String target = targets.isEmpty() ? null : targets.getFirst();
        if (target == null || actor.equals(target)) {
            lock(actor);
        } else {
            lockPair(actor, target);
        }
        var now = LocalDateTime.now(clock);
        var attempt = db.query("SELECT window_start,attempts FROM contact_request_attempt WHERE username=?",
                (r,n) -> new Attempt(r.getTimestamp(1).toLocalDateTime(),r.getInt(2)), actor);
        if (attempt.isEmpty()) {
            db.update("INSERT INTO contact_request_attempt VALUES(?,?,1)", actor, now);
        } else if (!attempt.getFirst().start().isAfter(now.minusHours(1))) {
            db.update("UPDATE contact_request_attempt SET window_start=?,attempts=1 WHERE username=?", now, actor);
        } else {
            if (attempt.getFirst().count() >= 8) {
                throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "CONTACT_REQUEST_LIMIT", "Intenta de nuevo más tarde.");
            }
            db.update("UPDATE contact_request_attempt SET attempts=attempts+1 WHERE username=?", actor);
        }
        if (target == null) return Map.of("status", "UNAVAILABLE");
        if (actor.equals(target)) return Map.of("status", "OWN_CODE");
        if (!students(actor,target) || identities.blocked(actor,target)) return Map.of("status", "UNAVAILABLE");
        if (identities.contacts(actor,target)) return Map.of("status", "ALREADY_CONTACT");
        if (db.queryForObject("SELECT COUNT(*) FROM contact_request WHERE sender=? AND recipient=?",
                Long.class, actor, target) > 0) return Map.of("status", "PENDING");
        if (db.queryForObject("SELECT COUNT(*) FROM contact_request WHERE sender=? AND recipient=?",
                Long.class, target, actor) > 0) return Map.of("status", "INCOMING");
        String eventKey = java.util.UUID.randomUUID().toString();
        db.update("INSERT INTO contact_request(sender,recipient,created_at,notice_key) VALUES(?,?,?,?)",
                actor, target, now, eventKey);
        notifications.enqueue(target, actor, null, null, "CONTACT_REQUEST", eventKey);
        return Map.of("status", "SENT");
    }

    @GetMapping("/requests")
    public List<Invitation> incoming(Principal principal) {
        String actor = principal.getName();
        if (!identities.active(actor).role().equals("ALUMNO")) return List.of();
        return db.query("""
                SELECT r.sender,u.display_name FROM contact_request r JOIN main_user u ON u.username=r.sender
                WHERE r.recipient=? AND u.enabled=TRUE AND u.role='ALUMNO' ORDER BY r.created_at DESC LIMIT 50
                """, (r,n) -> new Invitation(r.getString(1),r.getString(2)), actor).stream()
                .filter(r -> !identities.blocked(actor,r.sender())).toList();
    }

    @PostMapping("/requests/{sender}")
    @Transactional
    public Map<String,String> respond(Principal principal, @PathVariable String sender, @RequestBody Decision decision) {
        String actor = principal.getName();
        identities.active(actor);
        if (actor.equals(sender)) throw DomainException.missing();
        String first = actor.compareTo(sender) < 0 ? actor : sender;
        String second = first.equals(actor) ? sender : actor;
        lockPair(actor, sender);
        if (db.update("DELETE FROM contact_request WHERE recipient=? AND sender=?", actor, sender) != 1) {
            throw DomainException.missing();
        }
        if (decision.accept()) {
            if (!students(actor,sender) || identities.blocked(actor,sender)) throw DomainException.missing();
            if (!identities.contacts(actor,sender)) {
                try {
                    db.update("INSERT INTO contact_link(user_a,user_b,approved_by) VALUES(?,?,?)", first, second, actor);
                    audit.record(actor,"CONTACT_REQUEST_ACCEPTED",first+":"+second);
                } catch (DuplicateKeyException existing) {
                    // A concurrent administrative approval already linked this pair.
                }
            }
            db.update("DELETE FROM contact_request WHERE sender=? AND recipient=?", actor, sender);
        }
        return Map.of("status",decision.accept()?"ACCEPTED":"DECLINED");
    }

    private boolean students(String first, String second) {
        return db.queryForObject("""
                SELECT COUNT(*) FROM main_user WHERE username IN (?,?) AND enabled=TRUE AND role='ALUMNO'
                """, Long.class, first, second) == 2;
    }

    private void lockPair(String first, String second) {
        if (first.compareTo(second) < 0) {
            lock(first);
            lock(second);
        } else {
            lock(second);
            lock(first);
        }
    }

    private void lock(String username) {
        if (db.queryForList("SELECT username FROM main_user WHERE username=? FOR UPDATE",String.class,username).isEmpty()) {
            throw DomainException.missing();
        }
    }

    public record Code(String code) {}
    public record PersonalCode(String code,List<String> qr) {}
    public record Decision(boolean accept) {}
    public record Invitation(String sender,String name) {}
    private record Attempt(LocalDateTime start,int count) {}
}
