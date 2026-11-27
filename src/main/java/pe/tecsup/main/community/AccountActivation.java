package pe.tecsup.main.community;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountActivation {
    private static final Set<String> ELIGIBLE_ROLES = Set.of("ALUMNO", "DOCENTE", "FAMILIAR");
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final Clock clock;

    AccountActivation(JdbcTemplate db, PasswordEncoder passwords, Clock clock) {
        this.db = db;
        this.passwords = passwords;
        this.clock = clock;
    }

    record Confirmation(String username, String code, String password) {
        @Override public String toString() { return "Confirmation[redacted]"; }
    }
    private record Account(String hash, String role, boolean enabled) {}

    @PostMapping("/api/access/activate")
    @Transactional
    public ResponseEntity<Void> activate(@RequestBody Confirmation input) {
        if (input.username() == null || !input.username().matches("[A-Za-z0-9._-]{3,60}") ||
                input.code() == null || !input.code().matches("[A-Za-z0-9_-]{32}")) {
            throw invalidCode();
        }
        if (input.password() == null || input.password().length() < 12 ||
                input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw DomainException.invalid("La contraseña necesita al menos 12 caracteres y máximo 72 bytes UTF-8.");
        }
        String username = input.username();
        Account account = db.query("""
                SELECT password_hash,role,enabled FROM main_user WHERE username=? FOR UPDATE
                """, (row, index) -> new Account(row.getString(1), row.getString(2), row.getBoolean(3)),
                username).stream().findFirst().orElseThrow(AccountActivation::invalidCode);
        if (!account.enabled() || !ELIGIBLE_ROLES.contains(account.role())) {
            throw invalidCode();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int used = db.update("""
                UPDATE account_activation_invitation SET used_at=?
                WHERE username=? AND token_hash=? AND credential_version=? AND used_at IS NULL AND expires_at>?
                """, now, username, digest(input.code()), digest(account.hash() + "\0" + account.role()), now);
        if (used != 1) {
            throw invalidCode();
        }
        db.update("UPDATE main_user SET password_hash=?, password_change_required=FALSE WHERE username=?",
                passwords.encode(input.password()), username);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private static DomainException invalidCode() {
        return DomainException.invalid("Código inválido, vencido o ya utilizado. Solicita otro al colegio.");
    }
}
