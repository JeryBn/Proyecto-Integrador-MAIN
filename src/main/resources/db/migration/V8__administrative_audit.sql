CREATE TABLE administrative_audit (
 id VARCHAR(36) PRIMARY KEY,
 actor VARCHAR(60) NOT NULL,
 action VARCHAR(40) NOT NULL,
 subject VARCHAR(200) NOT NULL,
 occurred_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_administrative_audit_time ON administrative_audit(occurred_at,id);
ALTER TABLE curso ADD COLUMN admin_version INTEGER NOT NULL DEFAULT 0;
