CREATE TABLE account_activation_invitation (
 username VARCHAR(60) PRIMARY KEY,
 token_hash CHAR(64) NOT NULL UNIQUE,
 credential_version CHAR(64) NOT NULL,
 expires_at DATETIME NOT NULL,
 used_at DATETIME NULL,
 FOREIGN KEY (username) REFERENCES main_user(username)
);
