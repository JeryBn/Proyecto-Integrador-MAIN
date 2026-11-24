ALTER TABLE conversation_member ADD COLUMN encrypted_joined_after BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_member ADD COLUMN encrypted_join_revision VARCHAR(36);
