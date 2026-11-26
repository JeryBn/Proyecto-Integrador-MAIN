CREATE TABLE automatic_assessment (
 id VARCHAR(64) PRIMARY KEY,
 policy_version VARCHAR(80) NOT NULL,
 subject_kind VARCHAR(16) NOT NULL,
 subject_id VARCHAR(36) NOT NULL,
 content_hash VARCHAR(64) NOT NULL,
 outcome VARCHAR(32) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 lease_until TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL
);
CREATE INDEX automatic_assessment_subject ON automatic_assessment(subject_kind,subject_id,content_hash);
