-- V12__add_booking_address_fields.sql
ALTER TABLE bookings
ADD COLUMN address_text VARCHAR(255),
ADD COLUMN latitude NUMERIC(10, 6),
ADD COLUMN longitude NUMERIC(10, 6);

CREATE INDEX idx_bookings_location ON bookings (latitude, longitude);
