-- ══════════════════════════════════════════════════════════════════════════
-- V14__seed_danang_gps_stadiums_data.sql 
-- LƯU Ý: Tên file được đặt là V14 (thay vì V9) vì cột latitude/longitude
-- chỉ mới được tạo ra ở file V12. Nếu đặt V9, Flyway sẽ báo lỗi Column Not Found!
-- Dữ liệu giả lập 5 Sân thể thao tại khu vực ĐÀ NẴNG để test UC-OV-02 / UC-OV-03
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Bổ sung thêm tiện ích "Đèn chiếu sáng ban đêm" nếu chưa có
INSERT INTO amenities (name, icon) 
VALUES ('Đèn chiếu sáng ban đêm', 'sun')
ON CONFLICT (name) DO NOTHING;

-- 2. CHÈN DỮ LIỆU SÂN VẬN ĐỘNG (STADIUMS) BẰNG BLOCK ẨN DANH (DO $$)
DO $$
DECLARE
    owner_id_val INT := 1;
    football_id INT := 1;
    badminton_id INT := 2;
    tennis_id INT := 3;
    
    stadium1_id INT;
    stadium2_id INT;
    stadium3_id INT;
    stadium4_id INT;
    stadium5_id INT;
BEGIN
    -- Sân 1: Tuyên Sơn (Trung tâm Hải Châu)
    IF NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Tuyên Sơn') THEN
        INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating, latitude, longitude)
        VALUES (owner_id_val, football_id, 'Sân Bóng Đá Tuyên Sơn', 'Sân trung tâm xịn nhất Đà Nẵng gần Cầu Rồng', 'Hải Châu, Đà Nẵng', 300000, 20, '05:00', '23:00', 'AVAILABLE', 4.8, 16.0352, 108.2241)
        RETURNING stadium_id INTO stadium1_id;
    END IF;

    -- Sân 2: Chuyên Việt (Cách trung tâm 1.5km)
    IF NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Chuyên Việt') THEN
        INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating, latitude, longitude)
        VALUES (owner_id_val, badminton_id, 'Sân Cầu Lông Chuyên Việt', 'Sân cầu lông đạt chuẩn thi đấu', 'Hải Châu, Đà Nẵng', 120000, 10, '05:00', '23:00', 'AVAILABLE', 4.5, 16.0441, 108.2163)
        RETURNING stadium_id INTO stadium2_id;
    END IF;

    -- Sân 3: Lê Quý Đôn (Qua cầu, cách trung tâm 3.5km)
    IF NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Lê Quý Đôn') THEN
        INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating, latitude, longitude)
        VALUES (owner_id_val, football_id, 'Sân Bóng Đá Lê Quý Đôn', 'Sân nhân tạo thoáng mát gần biển', 'Sơn Trà, Đà Nẵng', 250000, 15, '05:00', '23:00', 'AVAILABLE', 4.2, 16.0568, 108.2356)
        RETURNING stadium_id INTO stadium3_id;
    END IF;

    -- Sân 4: Bưu Điện (Cách trung tâm 2.5km)
    IF NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Tennis Bưu Điện') THEN
        INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating, latitude, longitude)
        VALUES (owner_id_val, tennis_id, 'Sân Tennis Bưu Điện', 'Khuôn viên bưu điện Thanh Khê', 'Thanh Khê, Đà Nẵng', 150000, 4, '05:00', '23:00', 'AVAILABLE', 4.6, 16.0594, 108.2113)
        RETURNING stadium_id INTO stadium4_id;
    END IF;

    -- Sân 5: Hòa Khánh (Ngoại ô, cách trung tâm 9km)
    IF NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Hòa Khánh') THEN
        INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating, latitude, longitude)
        VALUES (owner_id_val, football_id, 'Sân Bóng Đá Hòa Khánh', 'Sân sinh viên giá rẻ Liên Chiểu', 'Liên Chiểu, Đà Nẵng', 180000, 20, '05:00', '23:00', 'AVAILABLE', 4.0, 16.0682, 108.1483)
        RETURNING stadium_id INTO stadium5_id;
    END IF;

    -- Lấy lại ID nếu đã tồn tại
    IF stadium1_id IS NULL THEN SELECT stadium_id INTO stadium1_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Tuyên Sơn' LIMIT 1; END IF;
    IF stadium2_id IS NULL THEN SELECT stadium_id INTO stadium2_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Chuyên Việt' LIMIT 1; END IF;
    IF stadium3_id IS NULL THEN SELECT stadium_id INTO stadium3_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Lê Quý Đôn' LIMIT 1; END IF;
    IF stadium4_id IS NULL THEN SELECT stadium_id INTO stadium4_id FROM stadiums WHERE stadium_name = 'Sân Tennis Bưu Điện' LIMIT 1; END IF;
    IF stadium5_id IS NULL THEN SELECT stadium_id INTO stadium5_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Hòa Khánh' LIMIT 1; END IF;

    -- 3. CHÈN TIỆN ÍCH CHO CÁC SÂN (Phân bổ bất đối xứng để test AND logic)
    -- Sân 1: Có ĐẦY ĐỦ Wifi, Bãi đỗ xe, Căng tin, Thuê giày, Đèn chiếu sáng
    INSERT INTO stadium_amenities (stadium_id, amenity_id) 
    SELECT stadium1_id, amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Thuê giày', 'Đèn chiếu sáng ban đêm')
    ON CONFLICT DO NOTHING;

    -- Sân 2: CHỈ CÓ Wifi
    INSERT INTO stadium_amenities (stadium_id, amenity_id) 
    SELECT stadium2_id, amenity_id FROM amenities WHERE name = 'Wifi'
    ON CONFLICT DO NOTHING;

    -- Sân 3: CHỈ CÓ Bãi đỗ xe
    INSERT INTO stadium_amenities (stadium_id, amenity_id) 
    SELECT stadium3_id, amenity_id FROM amenities WHERE name = 'Bãi đỗ xe'
    ON CONFLICT DO NOTHING;

    -- 4. CHÈN TIME SLOTS (Dùng thời gian động của PostgreSQL)
    -- Hôm nay (Current Date) 17:00 - 19:00 và 19:00 - 21:00
    IF NOT EXISTS (SELECT 1 FROM time_slots WHERE stadium_id = stadium1_id AND start_time = CURRENT_DATE + TIME '17:00:00') THEN
        INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status) VALUES 
        (stadium1_id, CURRENT_DATE + TIME '17:00:00', CURRENT_DATE + TIME '19:00:00', 'AVAILABLE'),
        (stadium1_id, CURRENT_DATE + TIME '19:00:00', CURRENT_DATE + TIME '21:00:00', 'AVAILABLE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM time_slots WHERE stadium_id = stadium2_id AND start_time = CURRENT_DATE + TIME '17:00:00') THEN
        INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status) VALUES 
        (stadium2_id, CURRENT_DATE + TIME '17:00:00', CURRENT_DATE + TIME '19:00:00', 'AVAILABLE');
    END IF;

    -- Ngày mai (Current Date + 1 Day) 17:00 - 19:00
    IF NOT EXISTS (SELECT 1 FROM time_slots WHERE stadium_id = stadium3_id AND start_time = CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00') THEN
        INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status) VALUES 
        (stadium3_id, CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00', CURRENT_DATE + INTERVAL '1 day' + TIME '19:00:00', 'AVAILABLE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM time_slots WHERE stadium_id = stadium4_id AND start_time = CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00') THEN
        INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status) VALUES 
        (stadium4_id, CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00', CURRENT_DATE + INTERVAL '1 day' + TIME '19:00:00', 'AVAILABLE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM time_slots WHERE stadium_id = stadium5_id AND start_time = CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00') THEN
        INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status) VALUES 
        (stadium5_id, CURRENT_DATE + INTERVAL '1 day' + TIME '17:00:00', CURRENT_DATE + INTERVAL '1 day' + TIME '19:00:00', 'AVAILABLE');
    END IF;

END $$;
