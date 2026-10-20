CREATE TABLE message_attachment (
 message_id VARCHAR(36) PRIMARY KEY,
 kind VARCHAR(12) NOT NULL,
 mime VARCHAR(60) NOT NULL,
 file_name VARCHAR(100) NOT NULL,
 byte_size INTEGER NOT NULL,
 source_hash VARCHAR(64) NOT NULL,
 content_hash VARCHAR(64) NOT NULL,
 FOREIGN KEY(message_id) REFERENCES message(id)
);
CREATE TABLE message_attachment_chunk (
 message_id VARCHAR(36) NOT NULL,
 sequence_no INTEGER NOT NULL,
 payload BLOB NOT NULL,
 PRIMARY KEY(message_id,sequence_no),
 FOREIGN KEY(message_id) REFERENCES message_attachment(message_id)
);
