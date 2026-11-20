CREATE TABLE moderation_retry (
    subject_kind VARCHAR(12) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL,
    retry_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (subject_kind, subject_id, content_hash)
);
CREATE INDEX moderation_retry_updated ON moderation_retry(updated_at);
