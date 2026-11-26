CREATE TABLE account_contact (
 username VARCHAR(60) PRIMARY KEY REFERENCES main_user(username),
 email VARCHAR(254) NOT NULL UNIQUE,
 verified_at TIMESTAMP NULL,
 approved_by VARCHAR(60) REFERENCES main_user(username),
 created_at TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL
);

CREATE TABLE account_access_token (
 id VARCHAR(36) PRIMARY KEY,
 username VARCHAR(60) NOT NULL REFERENCES main_user(username),
 token_hash CHAR(64) NOT NULL UNIQUE,
 purpose VARCHAR(24) NOT NULL,
 delivery VARCHAR(12) NOT NULL,
 credential_version CHAR(64) NOT NULL,
 expires_at TIMESTAMP NOT NULL,
 used_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL,
 CHECK(purpose IN ('ACTIVATE','RESET','VERIFY_FAMILY','VERIFY_CONTACT')),
 CHECK(delivery IN ('EMAIL','SCHOOL'))
);
CREATE INDEX account_access_owner ON account_access_token(username,purpose,expires_at);
