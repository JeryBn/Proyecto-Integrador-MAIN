package pe.tecsup.main.community;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/api/v1/chat-identity")
class ChatDeviceIdentity {
    private static final byte[] ED25519_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");
    static final String PROOF_DOMAIN = "MAIN chat device identity v1";
    static final String ENCRYPTION_DOMAIN = "MAIN chat encryption key v1";
    private final JdbcTemplate db;
    private final IdentityService users;
    private final CommunicationService chats;

    ChatDeviceIdentity(JdbcTemplate db, IdentityService users, CommunicationService chats) {
        this.db = db;
        this.users = users;
        this.chats = chats;
    }

    record Registration(String signingKey, String proof, String encryptionKey) {}
    record Device(String username, String deviceId, String signingKey, String encryptionKey, String encryptionProof) {}

    @PutMapping("/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(Principal principal, @PathVariable UUID deviceId, @RequestBody Registration input) {
        String username = principal.getName();
        users.active(username);
        verify(username, deviceId, input);
        try {
            db.update("INSERT INTO chat_device_identity (username,device_id,signing_key,encryption_key,encryption_proof) VALUES (?,?,?,?,?)",
                    username, deviceId.toString(), input.signingKey(), input.encryptionKey(),
                    input.encryptionKey() == null ? null : input.proof());
        } catch (DuplicateKeyException duplicate) {
            Device registered = registeredDevice(username, deviceId);
            if (!input.signingKey().equals(registered.signingKey())) {
                throw keyConflict();
            }
            if (input.encryptionKey() == null) {
                return;
            }
            if (registered.encryptionKey() == null) {
                int updated = db.update("""
                        UPDATE chat_device_identity SET encryption_key=?, encryption_proof=?
                        WHERE username=? AND device_id=? AND encryption_key IS NULL
                        """, input.encryptionKey(), input.proof(), username, deviceId.toString());
                if (updated == 1) {
                    return;
                }
                registered = registeredDevice(username, deviceId);
            }
            if (!input.encryptionKey().equals(registered.encryptionKey())) {
                throw keyConflict();
            }
        }
    }

    private Device registeredDevice(String username, UUID deviceId) {
        return db.queryForObject("""
                SELECT username,device_id,signing_key,encryption_key,encryption_proof
                FROM chat_device_identity WHERE username=? AND device_id=?
                """, ChatDeviceIdentity::readDevice, username, deviceId.toString());
    }

    private DomainException keyConflict() {
        return new DomainException(HttpStatus.CONFLICT, "CHAT_DEVICE_KEY_CHANGED",
                "Este dispositivo ya tiene una clave de identidad o cifrado registrada.");
    }

    @GetMapping("/conversations/{conversationId}/devices")
    public ResponseEntity<List<Device>> devices(Principal principal, @PathVariable String conversationId) {
        String actor = principal.getName();
        if (!"DIRECT".equals(chats.conversation(actor, conversationId).kind())) {
            throw DomainException.missing();
        }
        chats.authorizeAttachment(actor, conversationId);
        List<Device> devices = db.query("""
                SELECT d.username,d.device_id,d.signing_key,d.encryption_key,d.encryption_proof FROM chat_device_identity d
                JOIN conversation_member m ON m.username=d.username
                WHERE m.conversation_id=? AND m.state='ACTIVE'
                ORDER BY d.username,d.device_id
                """, ChatDeviceIdentity::readDevice, conversationId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(devices);
    }

    private static Device readDevice(java.sql.ResultSet row, int index) throws java.sql.SQLException {
        return new Device(row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5));
    }

    private static void verify(String username, UUID deviceId, Registration input) {
        try {
            byte[] key = decode(input.signingKey(), 32, 43);
            byte[] proof = decode(input.proof(), 64, 86);
            byte[] encoded = new byte[ED25519_PREFIX.length + key.length];
            System.arraycopy(ED25519_PREFIX, 0, encoded, 0, ED25519_PREFIX.length);
            System.arraycopy(key, 0, encoded, ED25519_PREFIX.length, key.length);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded)));
            String payload = String.join("\n", PROOF_DOMAIN, username, deviceId.toString(), input.signingKey());
            if (input.encryptionKey() != null) {
                byte[] encryptionKey = decode(input.encryptionKey(), 65, 87);
                if (encryptionKey[0] != 4) {
                    throw new IllegalArgumentException();
                }
                payload = String.join("\n", ENCRYPTION_DOMAIN, username, deviceId.toString(),
                        input.signingKey(), input.encryptionKey());
            }
            verifier.update(payload.getBytes(UTF_8));
            if (verifier.verify(proof)) {
                return;
            }
        } catch (IllegalArgumentException | GeneralSecurityException invalid) {
            throw DomainException.invalid("La prueba de la clave de identidad no es válida.");
        }
        throw DomainException.invalid("La prueba de la clave de identidad no es válida.");
    }

    private static byte[] decode(String input, int bytes, int characters) {
        if (input == null || input.length() != characters) {
            throw new IllegalArgumentException();
        }
        byte[] decoded = Base64.getUrlDecoder().decode(input);
        if (decoded.length != bytes || !Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(input)) {
            throw new IllegalArgumentException();
        }
        return decoded;
    }
}
