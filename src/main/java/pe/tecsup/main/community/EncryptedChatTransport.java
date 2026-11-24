package pe.tecsup.main.community;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
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
        store(conversationId, messageId, sender, senderDevice, recipient, recipientDevice,
                input.encapsulatedKey(), input.ciphertext());
    }

    record GroupRecipient(String username, String deviceId, String signingKey, String encryptionKey,
                          String encryptionProof, long joinedAfter, String joinRevision) {}
    record GroupRoster(String revision, List<GroupRecipient> devices) {}
    record GroupEnvelope(String username, UUID deviceId, String encapsulatedKey, String ciphertext) {}
    record GroupSend(UUID messageId, UUID senderDeviceId, String revision, List<GroupEnvelope> envelopes) {}

    @GetMapping("/group-recipients")
    public ResponseEntity<GroupRoster> groupRecipients(Principal principal, @PathVariable String conversationId) {
        String actor = principal.getName();
        requireGroup(actor, conversationId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(groupRoster(actor, conversationId));
    }

    @PostMapping("/group")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendGroup(Principal principal, @PathVariable String conversationId, @RequestBody GroupSend input) {
        String actor = principal.getName();
        requireGroup(actor, conversationId);
        if (input.messageId() == null || input.senderDeviceId() == null || input.envelopes() == null ||
                input.envelopes().isEmpty() || input.envelopes().size() > 64) {
            throw DomainException.invalid("El lote de mensajes cifrados no es válido.");
        }
        db.queryForObject("SELECT id FROM conversation WHERE id=? FOR UPDATE", String.class, conversationId);
        requireGroup(actor, conversationId);
        requireDevice(actor, input.senderDeviceId().toString());
        GroupRoster roster = groupRoster(actor, conversationId);
        if (!roster.revision().equals(input.revision()) || roster.devices().size() != input.envelopes().size()) {
            throw groupConflict();
        }
        var envelopes = new HashMap<List<String>, GroupEnvelope>();
        for (GroupEnvelope envelope : input.envelopes()) {
            if (envelope == null || envelope.username() == null || envelope.deviceId() == null ||
                    !isEnvelope(envelope.encapsulatedKey(), envelope.ciphertext()) ||
                    envelopes.put(List.of(envelope.username(), envelope.deviceId().toString()), envelope) != null) {
                throw DomainException.invalid("Un mensaje cifrado del lote no es válido.");
            }
        }
        for (GroupRecipient recipient : roster.devices()) {
            GroupEnvelope envelope = envelopes.get(List.of(recipient.username(), recipient.deviceId()));
            if (envelope == null) {
                throw groupConflict();
            }
            store(conversationId, input.messageId().toString(), actor, input.senderDeviceId().toString(),
                    recipient.username(), recipient.deviceId(), envelope.encapsulatedKey(), envelope.ciphertext());
        }
    }

    private GroupRoster groupRoster(String actor, String conversationId) {
        List<GroupRecipient> devices = db.query("""
                SELECT m.username,d.device_id,d.signing_key,d.encryption_key,d.encryption_proof,m.encrypted_joined_after,m.encrypted_join_revision
                FROM conversation_member m JOIN main_user u ON u.username=m.username
                LEFT JOIN chat_device_identity d ON d.username=m.username AND d.encryption_key IS NOT NULL
                WHERE m.conversation_id=? AND m.state='ACTIVE' AND u.enabled=TRUE AND m.username<>?
                ORDER BY m.username,d.device_id LIMIT 65
                """, (row, index) -> new GroupRecipient(row.getString(1), row.getString(2), row.getString(3),
                row.getString(4), row.getString(5), row.getLong(6), row.getString(7)), conversationId, actor);
        if (devices.isEmpty() || devices.size() > 64 || devices.stream().anyMatch(device -> device.deviceId() == null)) {
            throw new DomainException(HttpStatus.CONFLICT, "GROUP_DEVICES_UNAVAILABLE",
                    "Los miembros del grupo deben tener un dispositivo de chat cifrado disponible.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (GroupRecipient device : devices) {
                byte[] identity = device.username().getBytes(StandardCharsets.UTF_8);
                digest.update(java.nio.ByteBuffer.allocate(Integer.BYTES).putInt(identity.length).array());
                digest.update(identity);
                digest.update(device.deviceId().getBytes(StandardCharsets.US_ASCII));
                String joinRevision = device.joinRevision() == null ? "00000000-0000-0000-0000-000000000000" : device.joinRevision();
                digest.update(joinRevision.getBytes(StandardCharsets.US_ASCII));
            }
            return new GroupRoster(HexFormat.of().formatHex(digest.digest()), devices);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private void requireGroup(String actor, String conversationId) {
        if (!"GROUP".equals(chats.conversation(actor, conversationId).kind())) {
            throw DomainException.missing();
        }
        chats.authorizeAttachment(actor, conversationId);
    }

    private DomainException groupConflict() {
        return new DomainException(HttpStatus.CONFLICT, "GROUP_DEVICES_CHANGED",
                "Los dispositivos del grupo cambiaron. Actualiza la lista antes de enviar.");
    }

    private void store(String conversationId, String messageId, String sender, String senderDevice,
                       String recipient, String recipientDevice, String encapsulatedKey, String ciphertext) {
        try {
            db.update("""
                    INSERT INTO encrypted_chat_envelope
                    (conversation_id,message_id,sender,sender_device,recipient,recipient_device,encapsulated_key,ciphertext)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, conversationId, messageId, sender, senderDevice, recipient, recipientDevice,
                    encapsulatedKey, ciphertext);
        } catch (DuplicateKeyException duplicate) {
            Stored stored = db.queryForObject("""
                    SELECT conversation_id,encapsulated_key,ciphertext FROM encrypted_chat_envelope
                    WHERE sender=? AND sender_device=? AND message_id=? AND recipient=? AND recipient_device=?
                    """, (row, index) -> new Stored(row.getString(1), row.getString(2), row.getString(3)),
                    sender, senderDevice, messageId, recipient, recipientDevice);
            if (!new Stored(conversationId, encapsulatedKey, ciphertext).equals(stored)) {
                throw new DomainException(HttpStatus.CONFLICT, "ENVELOPE_CONFLICT",
                        "Este identificador ya corresponde a otro mensaje cifrado.");
            }
        }
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<List<Received>> receive(Principal principal, @PathVariable String conversationId,
            @PathVariable UUID deviceId, @RequestParam(defaultValue = "0") long after) {
        String actor = principal.getName();
        if ("GROUP".equals(chats.conversation(actor, conversationId).kind())) {
            requireGroup(actor, conversationId);
        } else {
            directPeer(actor, conversationId);
        }
        requireDevice(actor, deviceId.toString());
        if (after < 0) {
            throw DomainException.invalid("El cursor no es válido.");
        }
        List<Received> envelopes = db.query("""
                SELECT e.sequence_id,e.sender,e.sender_device,e.message_id,e.encapsulated_key,e.ciphertext
                FROM encrypted_chat_envelope e JOIN conversation_member m
                  ON m.conversation_id=e.conversation_id AND m.username=e.recipient
                WHERE e.conversation_id=? AND e.recipient=? AND e.recipient_device=? AND e.sequence_id>?
                  AND m.state='ACTIVE' AND e.sequence_id>m.encrypted_joined_after
                ORDER BY e.sequence_id LIMIT 30
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
