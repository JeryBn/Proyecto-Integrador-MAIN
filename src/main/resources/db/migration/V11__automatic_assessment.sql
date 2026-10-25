CREATE TABLE automatic_assessment (
 id VARCHAR(64) PRIMARY KEY,
 policy_version VARCHAR(80) NOT NULL,
 outcome VARCHAR(32) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 lease_until TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL
);
