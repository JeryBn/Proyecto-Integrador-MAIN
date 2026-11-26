CREATE TABLE notification (
 id VARCHAR(36) PRIMARY KEY,
 recipient VARCHAR(100) NOT NULL REFERENCES main_user(username),
 sender VARCHAR(100) NOT NULL REFERENCES main_user(username),
 conversation_id VARCHAR(36) NOT NULL REFERENCES conversation(id),
 message_id VARCHAR(36) REFERENCES message(id),
 kind VARCHAR(20) NOT NULL,
 event_key VARCHAR(100) NOT NULL,
 created_at TIMESTAMP NOT NULL,
 read_at TIMESTAMP,
 UNIQUE(recipient,event_key)
);
CREATE INDEX notification_recipient ON notification(recipient,created_at);
