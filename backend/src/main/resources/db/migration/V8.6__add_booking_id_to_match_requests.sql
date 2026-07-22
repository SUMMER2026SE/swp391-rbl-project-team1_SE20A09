ALTER TABLE match_requests ADD COLUMN booking_id INT NULL;
ALTER TABLE match_requests ADD CONSTRAINT fk_match_requests_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL;
CREATE INDEX idx_match_requests_booking_id ON match_requests(booking_id);
CREATE UNIQUE INDEX uq_match_requests_active_booking ON match_requests(booking_id) WHERE match_status IN ('OPEN','FULL') AND booking_id IS NOT NULL;
