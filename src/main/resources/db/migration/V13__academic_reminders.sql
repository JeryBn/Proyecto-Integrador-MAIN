ALTER TABLE notification ADD COLUMN activity_id BIGINT;
ALTER TABLE notification ADD COLUMN reminder_due TIMESTAMP;
ALTER TABLE notification ADD COLUMN reminder_minutes INTEGER;
CREATE TABLE reminder_preference (
 owner VARCHAR(60) PRIMARY KEY REFERENCES main_user(username),
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 minutes_before INTEGER NOT NULL DEFAULT 60
);
CREATE INDEX notification_activity_reminder ON notification(recipient,activity_id,reminder_due,reminder_minutes);
