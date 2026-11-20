CREATE TABLE attendance_justification (
    id VARCHAR(36) PRIMARY KEY,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_date DATE NOT NULL,
    session_no SMALLINT NOT NULL,
    requested_by VARCHAR(60) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    state VARCHAR(12) NOT NULL,
    decision_note VARCHAR(300),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    UNIQUE (course_id, student_id, class_date, session_no),
    FOREIGN KEY (course_id, student_id, class_date, session_no)
        REFERENCES course_attendance(course_id, student_id, class_date, session_no)
);
