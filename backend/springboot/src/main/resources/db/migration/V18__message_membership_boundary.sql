ALTER TABLE message ADD COLUMN conversation_sequence BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_member ADD COLUMN joined_after_sequence BIGINT NOT NULL DEFAULT -1;
CREATE INDEX idx_message_conversation_sequence ON message(conversation_id,conversation_sequence);
