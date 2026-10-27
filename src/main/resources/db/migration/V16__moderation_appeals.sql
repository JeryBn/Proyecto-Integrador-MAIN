CREATE TABLE moderation_appeal (
 id VARCHAR(36) PRIMARY KEY, owner VARCHAR(60) NOT NULL,
 subject_kind VARCHAR(16) NOT NULL, subject_id VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL, content_hash VARCHAR(64) NOT NULL,
 request_reason VARCHAR(1000) NOT NULL, state VARCHAR(16) NOT NULL,
 created_at TIMESTAMP NOT NULL, decision_reason VARCHAR(1000),
 decided_by VARCHAR(60), decided_at TIMESTAMP,
 UNIQUE(subject_kind,subject_id,revision,content_hash),
 FOREIGN KEY(owner) REFERENCES main_user(username)
);
CREATE INDEX idx_appeal_owner ON moderation_appeal(owner,created_at,id);
CREATE INDEX idx_appeal_open ON moderation_appeal(state,subject_kind,subject_id);
