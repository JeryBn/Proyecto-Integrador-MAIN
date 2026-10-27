ALTER TABLE automatic_assessment ADD COLUMN coverage VARCHAR(40);
ALTER TABLE automatic_assessment ADD COLUMN categories_json VARCHAR(2000);
ALTER TABLE automatic_assessment ADD COLUMN sampled_frames INTEGER;
ALTER TABLE automatic_assessment ADD COLUMN duration_seconds DECIMAL(8,3);
