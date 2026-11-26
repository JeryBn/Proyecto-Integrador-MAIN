CREATE TABLE status_update (
 id VARCHAR(36) PRIMARY KEY, owner VARCHAR(60) NOT NULL, client_key VARCHAR(36) NOT NULL,
 body VARCHAR(700) NOT NULL, background VARCHAR(7) NOT NULL, audience VARCHAR(10) NOT NULL,
 kind VARCHAR(8) NOT NULL, state VARCHAR(12) NOT NULL, content_hash VARCHAR(64) NOT NULL,
 created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL,
 reason VARCHAR(300), reviewed_by VARCHAR(60), reviewed_at TIMESTAMP,
 UNIQUE(owner,client_key), FOREIGN KEY(owner) REFERENCES main_user(username),
 FOREIGN KEY(reviewed_by) REFERENCES main_user(username)
);
CREATE TABLE status_media_chunk (
 status_id VARCHAR(36) NOT NULL, sequence_no INTEGER NOT NULL, payload VARBINARY(60000) NOT NULL,
 PRIMARY KEY(status_id,sequence_no), FOREIGN KEY(status_id) REFERENCES status_update(id)
);
CREATE TABLE status_recipient (
 status_id VARCHAR(36) NOT NULL, username VARCHAR(60) NOT NULL,
 PRIMARY KEY(status_id,username), FOREIGN KEY(status_id) REFERENCES status_update(id),
 FOREIGN KEY(username) REFERENCES main_user(username)
);
CREATE TABLE status_view (
 status_id VARCHAR(36) NOT NULL, username VARCHAR(60) NOT NULL, viewed_at TIMESTAMP NOT NULL,
 PRIMARY KEY(status_id,username), FOREIGN KEY(status_id) REFERENCES status_update(id),
 FOREIGN KEY(username) REFERENCES main_user(username)
);
