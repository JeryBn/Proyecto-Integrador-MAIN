CREATE TABLE school_year (
 id INTEGER PRIMARY KEY,
 name VARCHAR(60) NOT NULL
);

CREATE TABLE school_period (
 id VARCHAR(36) PRIMARY KEY,
 year_id INTEGER NOT NULL REFERENCES school_year(id),
 name VARCHAR(60) NOT NULL,
 position INTEGER NOT NULL,
 UNIQUE(year_id, position),
 CHECK(position BETWEEN 1 AND 4)
);

CREATE TABLE school_classroom (
 id VARCHAR(36) PRIMARY KEY,
 year_id INTEGER NOT NULL REFERENCES school_year(id),
 level VARCHAR(12) NOT NULL,
 grade INTEGER NOT NULL,
 section VARCHAR(20) NOT NULL,
 grading_mode VARCHAR(12) NOT NULL,
 maximum DECIMAL(8,2) NOT NULL,
 passing_score DECIMAL(8,2) NOT NULL,
 passing_achievement VARCHAR(2) NOT NULL,
 UNIQUE(year_id, level, grade, section),
 CHECK(level IN ('PRIMARIA','SECUNDARIA')),
 CHECK((level='PRIMARIA' AND grade BETWEEN 1 AND 6) OR (level='SECUNDARIA' AND grade BETWEEN 1 AND 5)),
 CHECK(grading_mode IN ('NUMERIC','LITERAL')),
 CHECK(maximum>0 AND maximum<=1000 AND passing_score>=0 AND passing_score<=maximum),
 CHECK(passing_achievement IN ('AD','A','B','C'))
);

CREATE TABLE school_enrollment (
 id VARCHAR(36) PRIMARY KEY,
 year_id INTEGER NOT NULL REFERENCES school_year(id),
 classroom_id VARCHAR(36) NOT NULL REFERENCES school_classroom(id),
 student_id BIGINT NOT NULL REFERENCES alumno(id),
 created_at TIMESTAMP NOT NULL,
 UNIQUE(year_id, student_id)
);

CREATE TABLE school_course (
 id VARCHAR(36) PRIMARY KEY,
 classroom_id VARCHAR(36) NOT NULL REFERENCES school_classroom(id),
 course_id BIGINT NOT NULL REFERENCES curso(id),
 teacher_username VARCHAR(60) NOT NULL REFERENCES main_user(username),
 UNIQUE(classroom_id,course_id)
);

CREATE TABLE school_mark (
 id VARCHAR(36) PRIMARY KEY,
 enrollment_id VARCHAR(36) NOT NULL REFERENCES school_enrollment(id),
 assignment_id VARCHAR(36) NOT NULL REFERENCES school_course(id),
 period_id VARCHAR(36) NOT NULL REFERENCES school_period(id),
 assessment VARCHAR(100) NOT NULL,
 score DECIMAL(8,2),
 achievement VARCHAR(2),
 recorded_by VARCHAR(60) NOT NULL REFERENCES main_user(username),
 recorded_at TIMESTAMP NOT NULL,
 version INTEGER NOT NULL DEFAULT 0,
 UNIQUE(enrollment_id,assignment_id,period_id,assessment),
 CHECK((score IS NOT NULL AND achievement IS NULL) OR (score IS NULL AND achievement IN ('AD','A','B','C')))
);

CREATE TABLE school_attendance (
 id VARCHAR(36) PRIMARY KEY,
 enrollment_id VARCHAR(36) NOT NULL REFERENCES school_enrollment(id),
 attendance_date DATE NOT NULL,
 status VARCHAR(12) NOT NULL,
 note VARCHAR(500) NOT NULL,
 recorded_by VARCHAR(60) NOT NULL REFERENCES main_user(username),
 version INTEGER NOT NULL DEFAULT 0,
 UNIQUE(enrollment_id,attendance_date),
 CHECK(status IN ('PRESENT','LATE','ABSENT','JUSTIFIED'))
);

CREATE TABLE school_recovery (
 id VARCHAR(36) PRIMARY KEY,
 enrollment_id VARCHAR(36) NOT NULL REFERENCES school_enrollment(id),
 assignment_id VARCHAR(36) NOT NULL REFERENCES school_course(id),
 period_id VARCHAR(36) NOT NULL REFERENCES school_period(id),
 plan VARCHAR(2000) NOT NULL,
 due_date DATE NOT NULL,
 status VARCHAR(12) NOT NULL,
 result_score DECIMAL(8,2),
 result_achievement VARCHAR(2),
 recorded_by VARCHAR(60) NOT NULL REFERENCES main_user(username),
 updated_at TIMESTAMP NOT NULL,
 version INTEGER NOT NULL DEFAULT 0,
 UNIQUE(enrollment_id,assignment_id,period_id),
 CHECK(status IN ('PENDING','IN_PROGRESS','COMPLETED'))
);

CREATE INDEX school_mark_period ON school_mark(period_id,assignment_id);
CREATE INDEX school_attendance_date ON school_attendance(attendance_date,enrollment_id);
