CREATE TABLE push_device (
 id VARCHAR(36) PRIMARY KEY,
 owner VARCHAR(60) NOT NULL REFERENCES main_user(username),
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 token_cipher VARCHAR(6000) NOT NULL,
 binding VARCHAR(36) NOT NULL,
 session_hash VARCHAR(64) NOT NULL,
 session_timeout INTEGER NOT NULL,
 registered_at TIMESTAMP NOT NULL,
 expires_at TIMESTAMP NOT NULL
);
CREATE TABLE push_delivery (
 device_id VARCHAR(36) NOT NULL REFERENCES push_device(id),
 notice_id VARCHAR(36) NOT NULL REFERENCES notification(id),
 state VARCHAR(16) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt TIMESTAMP NOT NULL,
 lease_until TIMESTAMP,
 PRIMARY KEY(device_id,notice_id)
);
CREATE INDEX push_delivery_due ON push_delivery(state,next_attempt);
