CREATE TABLE main_user (
 username VARCHAR(60) PRIMARY KEY, display_name VARCHAR(100) NOT NULL, role VARCHAR(16) NOT NULL,
 password_hash VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL, alumno_id BIGINT UNIQUE, docente_id BIGINT UNIQUE,
 dark_theme BOOLEAN NOT NULL DEFAULT FALSE, notifications BOOLEAN NOT NULL DEFAULT TRUE,
 status_audience VARCHAR(16) NOT NULL DEFAULT 'CONTACTS',
 FOREIGN KEY(alumno_id) REFERENCES alumno(id), FOREIGN KEY(docente_id) REFERENCES docente(id)
);
CREATE TABLE guardian_link (
 guardian VARCHAR(60) NOT NULL, alumno_id BIGINT NOT NULL, verified_by VARCHAR(60) NOT NULL,
 verified_at TIMESTAMP NOT NULL, PRIMARY KEY(guardian,alumno_id),
 FOREIGN KEY(guardian) REFERENCES main_user(username), FOREIGN KEY(alumno_id) REFERENCES alumno(id)
);
CREATE TABLE contact_link (
 user_a VARCHAR(60) NOT NULL, user_b VARCHAR(60) NOT NULL, approved_by VARCHAR(60) NOT NULL,
 PRIMARY KEY(user_a,user_b), FOREIGN KEY(user_a) REFERENCES main_user(username), FOREIGN KEY(user_b) REFERENCES main_user(username)
);
CREATE TABLE contact_preference (
 owner VARCHAR(60) NOT NULL, subject VARCHAR(60) NOT NULL, muted BOOLEAN NOT NULL, blocked BOOLEAN NOT NULL,
 PRIMARY KEY(owner,subject), FOREIGN KEY(owner) REFERENCES main_user(username), FOREIGN KEY(subject) REFERENCES main_user(username)
);
CREATE TABLE contact_report (
 id VARCHAR(36) PRIMARY KEY, reporter VARCHAR(60) NOT NULL, subject VARCHAR(60) NOT NULL,
 reason VARCHAR(1000) NOT NULL, created_at TIMESTAMP NOT NULL,
 FOREIGN KEY(reporter) REFERENCES main_user(username), FOREIGN KEY(subject) REFERENCES main_user(username)
);
CREATE TABLE timetable (
 id VARCHAR(36) PRIMARY KEY, curso_id BIGINT NOT NULL, day_of_week INTEGER NOT NULL,
 starts_at TIME NOT NULL, ends_at TIME NOT NULL, classroom VARCHAR(100) NOT NULL,
 FOREIGN KEY(curso_id) REFERENCES curso(id)
);
CREATE INDEX idx_guardian_student ON guardian_link(alumno_id);
CREATE INDEX idx_timetable_course ON timetable(curso_id,day_of_week,starts_at);
