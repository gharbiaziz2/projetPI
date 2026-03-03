-- Run this on your MySQL database (carthagevoyage) to add latitude, longitude and date to activite.
-- If you had "localisation" column before: run activite_localisation_to_latlon.sql to migrate.

ALTER TABLE activite ADD COLUMN latitude DOUBLE DEFAULT NULL;
ALTER TABLE activite ADD COLUMN longitude DOUBLE DEFAULT NULL;
ALTER TABLE activite ADD COLUMN date_activite DATE DEFAULT NULL;
