ALTER TABLE stadiums ADD COLUMN IF NOT EXISTS review_count INTEGER NOT NULL DEFAULT 0;

UPDATE stadiums s
SET review_count = (
SELECT COUNT(*) FROM reviews r
JOIN bookings b ON r.booking_id = b.booking_id
WHERE b.stadium_id = s.stadium_id
);
