-- Run on MySQL (carthagevoyage) to add longitude, latitude, image to hotel.

-- MySQL 8.0+: IF NOT EXISTS for ADD COLUMN. For older MySQL, run each line manually (ignore error if column exists).
ALTER TABLE hotel ADD COLUMN longitude DOUBLE NULL;
ALTER TABLE hotel ADD COLUMN latitude DOUBLE NULL;
ALTER TABLE hotel ADD COLUMN image VARCHAR(500) NULL;
