-- Add operating_time column to rides table
ALTER TABLE rides ADD COLUMN operating_time VARCHAR(20) NULL AFTER photo;

-- Add comment for the new column
ALTER TABLE rides MODIFY COLUMN operating_time VARCHAR(20) NULL COMMENT '운영 시간 (예: 09:00-18:00)';
