CREATE TABLE contact_code (
    username VARCHAR(60) PRIMARY KEY,
    code CHAR(6) NOT NULL UNIQUE,
    FOREIGN KEY (username) REFERENCES main_user(username)
);
CREATE TABLE contact_request_attempt (
    username VARCHAR(60) PRIMARY KEY,
    window_start TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL,
    FOREIGN KEY (username) REFERENCES main_user(username)
);
CREATE TABLE contact_request (
    sender VARCHAR(60) NOT NULL,
    recipient VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (sender, recipient),
    FOREIGN KEY (sender) REFERENCES main_user(username),
    FOREIGN KEY (recipient) REFERENCES main_user(username)
);
