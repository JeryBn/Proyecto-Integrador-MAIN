-- Existing revisions receive migration time, never the original message age.
ALTER TABLE message_revision ADD COLUMN submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE message_revision ADD COLUMN purged_at TIMESTAMP;
ALTER TABLE status_update ADD COLUMN retention_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE status_update ADD COLUMN purged_at TIMESTAMP;
CREATE TABLE moderation_retention_hold (
 subject_kind VARCHAR(16) NOT NULL, subject_id VARCHAR(36) NOT NULL,
 active BOOLEAN NOT NULL, reason VARCHAR(300) NOT NULL,
 opened_by VARCHAR(60) NOT NULL, opened_at TIMESTAMP NOT NULL,
 closed_by VARCHAR(60), closed_at TIMESTAMP,
 PRIMARY KEY(subject_kind,subject_id)
);
CREATE INDEX idx_revision_retention ON message_revision(purged_at,state,submitted_at);
CREATE INDEX idx_status_retention ON status_update(purged_at,state,retention_started_at);
