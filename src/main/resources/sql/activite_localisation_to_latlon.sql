-- Replace localisation with latitude and longitude in activite table.
-- Run on MySQL (carthagevoyage). Run each statement; ignore errors if column doesn't exist.

ALTER TABLE activite DROP COLUMN localisation;
ALTER TABLE activite ADD COLUMN latitude DOUBLE DEFAULT NULL;
ALTER TABLE activite ADD COLUMN longitude DOUBLE DEFAULT NULL;
