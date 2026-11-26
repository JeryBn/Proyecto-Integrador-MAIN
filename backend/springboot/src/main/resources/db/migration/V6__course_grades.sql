CREATE TABLE course_grade (
 id VARCHAR(36) PRIMARY KEY,
 course_id BIGINT NOT NULL REFERENCES curso(id),
 student_id BIGINT NOT NULL REFERENCES alumno(id),
 assessment VARCHAR(120) NOT NULL,
 score DECIMAL(8,2) NOT NULL,
 maximum DECIMAL(8,2) NOT NULL,
 recorded_by VARCHAR(100) NOT NULL REFERENCES main_user(username),
 recorded_at TIMESTAMP NOT NULL,
 UNIQUE(course_id,student_id,assessment),
 CHECK(maximum>0 AND score>=0 AND score<=maximum)
);
