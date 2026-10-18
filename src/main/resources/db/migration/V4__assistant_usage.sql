CREATE TABLE assistant_request (
 username VARCHAR(100) NOT NULL REFERENCES main_user(username),
 client_key VARCHAR(36) NOT NULL,
 question_hash VARCHAR(64) NOT NULL,
 state VARCHAR(20) NOT NULL,
 answer VARCHAR(16000),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(username,client_key)
);
CREATE INDEX assistant_usage ON assistant_request(username,created_at);
