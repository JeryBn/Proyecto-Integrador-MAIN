package pe.tecsup.main.community;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class ModerationRetry {
    static final String DELAY_MESSAGE = "La revisión por IA está demorada. MAIN volverá a intentarlo.";
    private static final Logger log = LoggerFactory.getLogger(ModerationRetry.class);
    private final JdbcTemplate db;
    private final Clock clock;

    ModerationRetry(JdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    void fail(String kind, String id, String hash) {
        LocalDateTime now = LocalDateTime.now(clock);
        var previous = db.queryForList("""
                SELECT attempts FROM moderation_retry
                WHERE subject_kind=? AND subject_id=? AND content_hash=?
                """, Integer.class, kind, id, hash);
        int attempts = previous.isEmpty() ? 1 : Math.min(30, previous.getFirst() + 1);
        long seconds = Math.min(300, 10L << Math.min(attempts - 1, 5));
        var retry = Timestamp.valueOf(now.plusSeconds(seconds));
        var updated = Timestamp.valueOf(now);
        if (previous.isEmpty()) {
            try {
                db.update("INSERT INTO moderation_retry VALUES(?,?,?,?,?,?)", kind, id, hash, attempts, retry, updated);
            } catch (DuplicateKeyException race) {
                // Another worker scheduled the same revision.
            }
        } else {
            db.update("""
                    UPDATE moderation_retry SET attempts=?,retry_at=?,updated_at=?
                    WHERE subject_kind=? AND subject_id=? AND content_hash=? AND attempts<=?
                    """, attempts, retry, updated, kind, id, hash, previous.getFirst());
        }
        if (attempts == 1 || attempts == 5) {
            log.warn("Moderation retry scheduled: type={}, attempts={}, retrySeconds={}", kind, attempts, seconds);
        }
    }

    void clear(String kind, String id, String hash) {
        db.update("DELETE FROM moderation_retry WHERE subject_kind=? AND subject_id=? AND content_hash=?", kind, id, hash);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3600000, initialDelay=3600000)
    void expireOld() {
        db.update("DELETE FROM moderation_retry WHERE updated_at<?", Timestamp.valueOf(LocalDateTime.now(clock).minusDays(2)));
    }
}
