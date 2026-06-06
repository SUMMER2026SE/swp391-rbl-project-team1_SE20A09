-- ── Add price_per_hour and capacity to stadiums ──────────────────────────────
ALTER TABLE stadiums ADD COLUMN price_per_hour DECIMAL(10, 2);
ALTER TABLE stadiums ADD COLUMN capacity INT;

-- Update existing stadiums with default values
UPDATE stadiums SET price_per_hour = 150000.00 WHERE price_per_hour IS NULL;
UPDATE stadiums SET capacity = 10 WHERE capacity IS NULL;
