CREATE TABLE encrypted_chat_envelope (
    sequence_id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    sender VARCHAR(60) NOT NULL,
    sender_device VARCHAR(36) NOT NULL,
    recipient VARCHAR(60) NOT NULL,
    recipient_device VARCHAR(36) NOT NULL,
    encapsulated_key VARCHAR(87) NOT NULL,
    ciphertext TEXT NOT NULL,
    PRIMARY KEY (sequence_id),
    UNIQUE (sender, sender_device, message_id, recipient, recipient_device),
    FOREIGN KEY (conversation_id) REFERENCES conversation(id),
    FOREIGN KEY (sender, sender_device) REFERENCES chat_device_identity(username, device_id),
    FOREIGN KEY (recipient, recipient_device) REFERENCES chat_device_identity(username, device_id)
);
CREATE INDEX idx_encrypted_chat_inbox ON encrypted_chat_envelope(conversation_id, recipient, recipient_device, sequence_id);
