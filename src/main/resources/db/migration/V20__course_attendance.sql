CREATE TABLE course_attendance (
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_date DATE NOT NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('PRESENT','ABSENT','LATE')),
    recorded_by VARCHAR(100) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    PRIMARY KEY (course_id, student_id, class_date),
    FOREIGN KEY (course_id) REFERENCES curso(id),
    FOREIGN KEY (student_id) REFERENCES alumno(id)
);
