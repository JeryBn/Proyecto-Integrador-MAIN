CREATE TABLE campus_post (
    id VARCHAR(36) PRIMARY KEY,
    author VARCHAR(60) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    state VARCHAR(12) NOT NULL CHECK (state IN ('PENDING','PUBLISHED','REJECTED')),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL,
    FOREIGN KEY (author) REFERENCES main_user(username)
);
CREATE INDEX campus_post_feed ON campus_post(state,created_at,id);
CREATE INDEX campus_post_author ON campus_post(author,created_at,id);
