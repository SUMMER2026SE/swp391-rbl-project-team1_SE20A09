-- ══════════════════════════════════════════════════════════════════════════
-- V8__seed_search_data.sql
-- Thêm dữ liệu tọa độ và tiện ích cho các sân đã có để test chức năng tìm kiếm
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Cập nhật tọa độ (Latitude, Longitude) cho các sân hiện có
-- Tọa độ giả lập xung quanh TP.HCM
UPDATE stadiums SET latitude = 10.850, longitude = 106.755 WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';
UPDATE stadiums SET latitude = 10.776, longitude = 106.700 WHERE stadium_name = 'Sân Cầu Lông Quận 1';
UPDATE stadiums SET latitude = 10.800, longitude = 106.711 WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';
UPDATE stadiums SET latitude = 10.732, longitude = 106.722 WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';
UPDATE stadiums SET latitude = 10.835, longitude = 106.678 WHERE stadium_name = 'Sân Bóng Đá Gò Vấp';

-- 2. Map tiện ích (Amenities) cho các sân
-- Lấy ID của các tiện ích
DO $$
DECLARE
    wifi_id INT;
    parking_id INT;
    canteen_id INT;
    shoes_id INT;
    locker_id INT;
    water_id INT;
    
    thuduc_id INT;
    quan1_id INT;
    binhthanh_id INT;
    phumyhung_id INT;
    govap_id INT;
BEGIN
    SELECT amenity_id INTO wifi_id FROM amenities WHERE name = 'Wifi';
    SELECT amenity_id INTO parking_id FROM amenities WHERE name = 'Bãi đỗ xe';
    SELECT amenity_id INTO canteen_id FROM amenities WHERE name = 'Căng tin';
    SELECT amenity_id INTO shoes_id FROM amenities WHERE name = 'Thuê giày';
    SELECT amenity_id INTO locker_id FROM amenities WHERE name = 'Phòng thay đồ';
    SELECT amenity_id INTO water_id FROM amenities WHERE name = 'Nước uống miễn phí';

    SELECT stadium_id INTO thuduc_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';
    SELECT stadium_id INTO quan1_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1';
    SELECT stadium_id INTO binhthanh_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';
    SELECT stadium_id INTO phumyhung_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';
    SELECT stadium_id INTO govap_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp';

    -- Sân Thủ Đức: Wifi, Bãi đỗ xe, Căng tin, Phòng thay đồ, Nước uống
    IF thuduc_id IS NOT NULL THEN
        INSERT INTO stadium_amenities (stadium_id, amenity_id) VALUES 
        (thuduc_id, wifi_id), (thuduc_id, parking_id), (thuduc_id, canteen_id), (thuduc_id, locker_id), (thuduc_id, water_id)
        ON CONFLICT DO NOTHING;
    END IF;

    -- Sân Quận 1: Wifi, Bãi đỗ xe, Thuê giày, Nước uống
    IF quan1_id IS NOT NULL THEN
        INSERT INTO stadium_amenities (stadium_id, amenity_id) VALUES 
        (quan1_id, wifi_id), (quan1_id, parking_id), (quan1_id, shoes_id), (quan1_id, water_id)
        ON CONFLICT DO NOTHING;
    END IF;
    
    -- Sân Bình Thạnh: Bãi đỗ xe, Căng tin, Nước uống
    IF binhthanh_id IS NOT NULL THEN
        INSERT INTO stadium_amenities (stadium_id, amenity_id) VALUES 
        (binhthanh_id, parking_id), (binhthanh_id, canteen_id), (binhthanh_id, water_id)
        ON CONFLICT DO NOTHING;
    END IF;
    
    -- Sân Gò Vấp: Wifi, Bãi đỗ xe, Căng tin, Thuê giày, Phòng thay đồ
    IF govap_id IS NOT NULL THEN
        INSERT INTO stadium_amenities (stadium_id, amenity_id) VALUES 
        (govap_id, wifi_id), (govap_id, parking_id), (govap_id, canteen_id), (govap_id, shoes_id), (govap_id, locker_id)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
