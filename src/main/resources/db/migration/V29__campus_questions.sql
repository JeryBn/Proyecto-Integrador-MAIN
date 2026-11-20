ALTER TABLE campus_post ADD COLUMN post_kind VARCHAR(12) NOT NULL DEFAULT 'POST';
ALTER TABLE campus_post ADD COLUMN parent_id VARCHAR(36);
ALTER TABLE campus_post ADD COLUMN withdrawn BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE campus_post ADD CONSTRAINT fk_campus_post_parent FOREIGN KEY (parent_id) REFERENCES campus_post(id);
CREATE INDEX campus_post_thread ON campus_post(parent_id,state,created_at);
