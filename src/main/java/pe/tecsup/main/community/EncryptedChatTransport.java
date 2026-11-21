package pe.tecsup.main.community;

import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name = "main.chat.encrypted-transport-preview", havingValue = "true")
@RequestMapping("/api/v1/conversations/{conversationId}/encrypted")
class EncryptedChatTransport {
    private final JdbcTemplate db;
    private final CommunicationService chats;

    EncryptedChatTransport(JdbcTemplate db, CommunicationService chats) {
        this.db = db;
        this.chats = chats;
    }

    record Send(UUID messageId, UUID senderDeviceId, UUID recipientDeviceId,
                int version, String encapsulatedKey, String ciphertext) {}
    record Received(long cursor, String sender, String senderDeviceId, String messageId,
                    int version, String encapsulatedKey, String ciphertext) {}
    record Stored(String conversationId, String encapsulatedKey, String ciphertext) {}

    @GetMapping("/availability")
    public ResponseEntity<Void> availability(Principal principal, @PathVariable String conversationId) {
        directPeer(principal.getName(), conversationId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void send(Principal principal, @PathVariable String conversationId, @RequestBody Send input) {
        String sender = principal.getName();
        String recipient = directPeer(sender, conversationId);
        if (input.messageId() == null || input.senderDeviceId() == null || input.recipientDeviceId() == null ||
                input.version() != 1 || !isEnvelope(input.encapsulatedKey(), input.ciphertext())) {
            throw DomainException.invalid("El mensaje cifrado tiene un formato inválido.");
        }
        String senderDevice = input.senderDeviceId().toString();
        String recipientDevice = input.recipientDeviceId().toString();
        String messageId = input.messageId().toString();
        // Keep allocation and commit order aligned for this conversation's inbox cursor.
        db.queryForObject("SELECT id FROM conversation WHERE id=? FOR UPDATE", String.class, conversationId);
        directPeer(sender, conversationId);
        requireDevice(sender, senderDevice);
        requireDevice(recipient, recipientDevice);
        try {
            db.update("""
                    INSERT INTO encrypted_chat_envelope
                    (conversation_id,message_id,sender,sender_device,recipient,recipient_device,encapsulated_key,ciphertext)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, conversationId, messageId, sender, senderDevice, recipient, recipientDevice,
                    input.encapsulatedKey(), input.ciphertext());
        } catch (DuplicateKeyException duplicate) {
            Stored stored = db.queryForObject("""
                    SELECT conversation_id,encapsulated_key,ciphertext FROM encrypted_chat_envelope
                    WHERE sender=? AND sender_device=? AND message_id=? AND recipient=? AND recipient_device=?
                    """, (row, index) -> new Stored(row.getString(1), row.getString(2), row.getString(3)),
                    sender, senderDevice, messageId, recipient, recipientDevice);
            if (!new Stored(conversationId, input.encapsulatedKey(), input.ciphertext()).equals(stored)) {
                throw new DomainException(HttpStatus.CONFLICT, "ENVELOPE_CONFLICT",
                        "Este identificador ya corresponde a otro mensaje cifrado.");
            }
        }
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<List<Received>> receive(Principal principal, @PathVariable String conversationId,
            @PathVariable UUID deviceId, @RequestParam(defaultValue = "0") long after) {
        String actor = principal.getName();
        directPeer(actor, conversationId);
        requireDevice(actor, deviceId.toString());
        if (after < 0) {
            throw DomainException.invalid("El cursor no es válido.");
        }
        List<Received> envelopes = db.query("""
                SELECT sequence_id,sender,sender_device,message_id,encapsulated_key,ciphertext
                FROM encrypted_chat_envelope
                WHERE conversation_id=? AND recipient=? AND recipient_device=? AND sequence_id>?
                ORDER BY sequence_id LIMIT 30
                """, (row, index) -> new Received(row.getLong(1), row.getString(2), row.getString(3),
                row.getString(4), 1, row.getString(5), row.getString(6)), conversationId, actor, deviceId.toString(), after);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(envelopes);
    }

    private String directPeer(String actor, String conversationId) {
        if (!"DIRECT".equals(chats.conversation(actor, conversationId).kind())) {
            throw DomainException.missing();
        }
        chats.authorizeAttachment(actor, conversationId);
        return db.queryForObject("""
                SELECT username FROM conversation_member
                WHERE conversation_id=? AND username<>? AND state='ACTIVE'
                """, String.class, conversationId, actor);
    }

    private void requireDevice(String username, String deviceId) {
        if (db.queryForObject("""
                SELECT COUNT(*) FROM chat_device_identity
                WHERE username=? AND device_id=? AND encryption_key IS NOT NULL
                """, Integer.class, username, deviceId) != 1) {
            throw DomainException.missing();
        }
    }

    private static boolean isEnvelope(String encapsulatedKey, String ciphertext) {
        try {
            byte[] key = decode(encapsulatedKey, 87);
            byte[] bytes = decode(ciphertext, 21867);
            return key.length == 65 && key[0] == 4 && bytes.length >= 17 && bytes.length <= 16400;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static byte[] decode(String value, int maxCharacters) {
        if (value == null || value.length() > maxCharacters) {
            throw new IllegalArgumentException();
        }
        byte[] decoded = Base64.getUrlDecoder().decode(value);
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(value)) {
            throw new IllegalArgumentException();
        }
        return decoded;
    }
}
