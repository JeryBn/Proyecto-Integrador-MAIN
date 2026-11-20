CREATE TABLE academic_year (
    year_number SMALLINT PRIMARY KEY,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    CHECK (starts_on < ends_on)
);
CREATE TABLE academic_period (
    year_number SMALLINT NOT NULL,
    period_no SMALLINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    PRIMARY KEY (year_number, period_no),
    FOREIGN KEY (year_number) REFERENCES academic_year(year_number),
    CHECK (starts_on <= ends_on)
);
CREATE TABLE school_section (
    id VARCHAR(36) PRIMARY KEY,
    year_number SMALLINT NOT NULL,
    grade VARCHAR(32) NOT NULL,
    label VARCHAR(16) NOT NULL,
    UNIQUE (year_number, grade, label),
    FOREIGN KEY (year_number) REFERENCES academic_year(year_number)
);
CREATE TABLE course_section (
    course_id BIGINT PRIMARY KEY,
    section_id VARCHAR(36) NOT NULL,
    FOREIGN KEY (course_id) REFERENCES curso(id),
    FOREIGN KEY (section_id) REFERENCES school_section(id)
);
