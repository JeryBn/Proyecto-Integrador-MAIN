ALTER TABLE campus_post ADD COLUMN course_id BIGINT;
ALTER TABLE campus_post ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE campus_post ADD CONSTRAINT fk_campus_post_course FOREIGN KEY (course_id) REFERENCES curso(id);
CREATE INDEX campus_post_course_state ON campus_post(course_id,state,created_at);
