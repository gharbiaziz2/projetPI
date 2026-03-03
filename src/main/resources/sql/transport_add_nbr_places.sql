-- Add nbr_places to transportlocal for seat/place management
ALTER TABLE transportlocal ADD COLUMN nbr_places INT NOT NULL DEFAULT 10;
