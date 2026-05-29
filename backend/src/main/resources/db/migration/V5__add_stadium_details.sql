-- V5__add_stadium_details.sql

-- 1. Thêm cột tọa độ GPS vào bảng stadiums
ALTER TABLE stadiums ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE stadiums ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- 2. Tạo bảng danh mục tiện ích
CREATE TABLE IF NOT EXISTS amenities (
    amenity_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 3. Tạo bảng nối N-N giữa sân bóng và tiện ích
CREATE TABLE IF NOT EXISTS stadium_amenities (
    stadium_id INT NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    amenity_id INT NOT NULL REFERENCES amenities(amenity_id) ON DELETE CASCADE,
    CONSTRAINT pk_stadium_amenities PRIMARY KEY (stadium_id, amenity_id)
);

-- 4. Tạo Index để tối ưu tìm kiếm
CREATE INDEX idx_stadium_amenities_stadium_id ON stadium_amenities(stadium_id);
CREATE INDEX idx_stadium_amenities_amenity_id ON stadium_amenities(amenity_id);

-- 5. Data seeder cho các tiện ích mặc định
INSERT INTO amenities (name) VALUES 
('Wifi'),
('Bãi đỗ xe'),
('Căng tin'),
('Đèn chiếu sáng'),
('Phòng thay đồ'),
('Cho thuê giày/vợt')
ON CONFLICT (name) DO NOTHING;
