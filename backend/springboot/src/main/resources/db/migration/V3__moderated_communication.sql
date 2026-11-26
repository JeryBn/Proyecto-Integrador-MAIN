CREATE TABLE conversation (
 id VARCHAR(36) PRIMARY KEY, kind VARCHAR(8) NOT NULL, title VARCHAR(100) NOT NULL,
 created_by VARCHAR(60) NOT NULL, created_at TIMESTAMP NOT NULL, direct_key VARCHAR(121) UNIQUE,
 FOREIGN KEY(created_by) REFERENCES main_user(username)
);
CREATE TABLE conversation_member (
 conversation_id VARCHAR(36) NOT NULL, username VARCHAR(60) NOT NULL, state VARCHAR(12) NOT NULL,
 administrator BOOLEAN NOT NULL, joined_at TIMESTAMP, PRIMARY KEY(conversation_id,username),
 FOREIGN KEY(conversation_id) REFERENCES conversation(id), FOREIGN KEY(username) REFERENCES main_user(username)
);
CREATE TABLE message (
 id VARCHAR(36) PRIMARY KEY, conversation_id VARCHAR(36) NOT NULL, sender VARCHAR(60) NOT NULL,
 client_key VARCHAR(36) NOT NULL, created_at TIMESTAMP NOT NULL, published_revision INTEGER,
 corrected_before BOOLEAN NOT NULL DEFAULT FALSE, edited_after BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE(sender,client_key), FOREIGN KEY(conversation_id) REFERENCES conversation(id), FOREIGN KEY(sender) REFERENCES main_user(username)
);
CREATE TABLE message_revision (
 message_id VARCHAR(36) NOT NULL, revision INTEGER NOT NULL, body VARCHAR(4000) NOT NULL,
 state VARCHAR(12) NOT NULL, reason VARCHAR(300), content_hash VARCHAR(64) NOT NULL,
 reviewed_by VARCHAR(60), reviewed_at TIMESTAMP, PRIMARY KEY(message_id,revision),
 FOREIGN KEY(message_id) REFERENCES message(id)
);
CREATE INDEX idx_message_conversation ON message(conversation_id,created_at,id);
CREATE INDEX idx_member_account ON conversation_member(username,state);
