CREATE TABLE course_period_close (
    course_id BIGINT NOT NULL,
    year_number SMALLINT NOT NULL,
    period_no SMALLINT NOT NULL,
    maximum DECIMAL(7,2) NOT NULL,
    closed_by VARCHAR(60) NOT NULL,
    closed_at TIMESTAMP NOT NULL,
    PRIMARY KEY(course_id,year_number,period_no),
    FOREIGN KEY(course_id) REFERENCES curso(id),
    FOREIGN KEY(year_number,period_no) REFERENCES academic_period(year_number,period_no)
);
CREATE TABLE course_period_grade (
    course_id BIGINT NOT NULL,
    year_number SMALLINT NOT NULL,
    period_no SMALLINT NOT NULL,
    student_id BIGINT NOT NULL,
    score DECIMAL(7,2) NOT NULL,
    PRIMARY KEY(course_id,year_number,period_no,student_id),
    FOREIGN KEY (course_id,year_number,period_no) REFERENCES course_period_close(course_id,year_number,period_no),
    FOREIGN KEY(student_id) REFERENCES alumno(id)
);
CREATE INDEX course_period_grade_student ON course_period_grade(student_id,year_number,period_no);
