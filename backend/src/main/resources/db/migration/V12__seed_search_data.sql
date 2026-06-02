-- ══════════════════════════════════════════════════════════════════════════
-- V12__seed_search_data.sql (was V13)
-- Add search features: coordinates, amenities table, junction table
-- ══════════════════════════════════════════════════════════════════════════

-- Add coordinates to stadiums
ALTER TABLE stadiums ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE stadiums ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Create amenities table
CREATE TABLE IF NOT EXISTS amenities (
    amenity_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(255)
);

-- Create junction table for stadium-amenities
CREATE TABLE IF NOT EXISTS stadium_amenities (
    stadium_id INT NOT NULL,
    amenity_id INT NOT NULL,
    PRIMARY KEY (stadium_id, amenity_id),
    CONSTRAINT fk_stadium_amenities_stadium FOREIGN KEY (stadium_id) REFERENCES stadiums (stadium_id) ON DELETE CASCADE,
    CONSTRAINT fk_stadium_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (amenity_id) ON DELETE CASCADE
);

-- Index for junction table
CREATE INDEX IF NOT EXISTS idx_stadium_amenities_stadium ON stadium_amenities(stadium_id);
CREATE INDEX IF NOT EXISTS idx_stadium_amenities_amenity ON stadium_amenities(amenity_id);

-- Seed some default amenities
INSERT INTO amenities (name, icon) VALUES
('Wifi', 'wifi'),
('Bãi đỗ xe', 'car'),
('Căng tin', 'coffee'),
('Thuê giày', 'shopping-bag'),
('Phòng thay đồ', 'user'),
('Nước uống miễn phí', 'droplet')
ON CONFLICT (name) DO NOTHING;
