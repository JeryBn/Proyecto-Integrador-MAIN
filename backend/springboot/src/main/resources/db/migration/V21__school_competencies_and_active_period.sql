ALTER TABLE school_mark ADD COLUMN competency VARCHAR(120) NOT NULL DEFAULT 'General';
ALTER TABLE school_year ADD COLUMN active_period_id VARCHAR(36) REFERENCES school_period(id);
