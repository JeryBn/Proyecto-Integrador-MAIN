CREATE TABLE school_publication (
 id VARCHAR(36) PRIMARY KEY,
 course_id BIGINT NOT NULL REFERENCES curso(id),
 classroom_id VARCHAR(36) REFERENCES school_classroom(id),
 teacher_username VARCHAR(60) NOT NULL REFERENCES main_user(username),
 kind VARCHAR(12) NOT NULL,
 title VARCHAR(160) NOT NULL,
 instructions VARCHAR(2000) NOT NULL,
 due_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL,
 CHECK(kind IN ('NOTICE','TASK','EXAM')),
 CHECK(kind='NOTICE' OR due_at IS NOT NULL)
);
CREATE TABLE school_publication_read (
 publication_id VARCHAR(36) NOT NULL REFERENCES school_publication(id),
 guardian VARCHAR(60) NOT NULL REFERENCES main_user(username),
 read_at TIMESTAMP NOT NULL,
 PRIMARY KEY(publication_id,guardian)
);
CREATE INDEX school_publication_course_date ON school_publication(course_id,created_at);
