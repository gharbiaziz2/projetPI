-- Run on MySQL (carthagevoyage) to add photo column to activite table.

ALTER TABLE activite ADD COLUMN photo VARCHAR(500) DEFAULT NULL;
