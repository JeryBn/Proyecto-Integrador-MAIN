CREATE TABLE chat_device_identity (
    username VARCHAR(60) NOT NULL,
    device_id VARCHAR(36) NOT NULL,
    signing_key VARCHAR(43) NOT NULL,
    PRIMARY KEY (username, device_id),
    FOREIGN KEY (username) REFERENCES main_user(username)
);
