-- ══════════════════════════════════════════════════════════════════════════
-- V114__seed_curated_demo_venues.sql — 11 complex thật thay thế bộ dữ liệu đã
-- xóa ở V113: 5 Đà Nẵng, 3 Hà Nội, 3 TP.HCM, chia đều cho 2 owner đã duyệt
-- (Sport Venue Owner Corp / Huy Sport Center).
--
-- Tên, toạ độ thật lấy từ scripts/output/overpass-{da-nang,ha-noi,ho-chi-minh}.json
-- (Overpass API, dữ liệu OSM/ODbL). Địa chỉ chi tiết là suy luận hợp lý từ vị
-- trí thật (OSM không trả địa chỉ số nhà đầy đủ cho các điểm này).
-- Ảnh (cover + gallery) là ảnh stock thật lấy từ Pexels
-- (scripts/output/pexels-images.json theo môn thể thao) — KHÔNG phải ảnh chụp
-- đúng địa điểm cụ thể (không có nguồn hợp pháp cho việc này), cùng cách làm
-- đã dùng ở V7.6.1.
-- Giá thuê/giờ, giờ mở cửa, phụ kiện là dữ liệu giả lập hợp lý cho mục đích demo.
-- ══════════════════════════════════════════════════════════════════════════

-- Cung Thể thao Tiên Sơn (Đà Nẵng) — Badminton + Basketball
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Cung Thể thao Tiên Sơn',
        'Nhà thi đấu đa năng trung tâm Đà Nẵng, có sân cầu lông và bóng rổ tiêu chuẩn thi đấu.',
        'Đường Phan Đăng Lưu, Hải Châu, Đà Nẵng', NULL,
        16.0367619, 108.2265377,
        'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Đà Nẵng', 'Hải Châu'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4931355/pexels-photo-4931355.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Khu cầu lông', 'Nhà thi đấu đa năng trung tâm Đà Nẵng, có sân cầu lông và bóng rổ tiêu chuẩn thi đấu.', 'FACILITY', complex_id,
        '06:00', '22:00', 70000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        70000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Khu bóng rổ', 'Nhà thi đấu đa năng trung tâm Đà Nẵng, có sân cầu lông và bóng rổ tiêu chuẩn thi đấu.', 'FACILITY', complex_id,
        '06:00', '22:00', 160000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        160000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Badminton', 'Basketball')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Vợt cầu lông (thuê)', 30000.00, 10, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Cầu lông (ống 12 quả)', 85000.00, 15, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng rổ (thuê)', 30000.00, 8, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Áo thi đấu', 25000.00, 10, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

-- Sân vận động Chi Lăng (Đà Nẵng) — Football
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động Chi Lăng',
        'Sân vận động lâu đời của Đà Nẵng, hiện khai thác các sân bóng đá mini cho thuê theo giờ.',
        'Đường Lê Duẩn, Hải Châu, Đà Nẵng', NULL,
        15.9992598, 108.2233504,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Đà Nẵng', 'Hải Châu'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Khu sân bóng đá', 'Sân vận động lâu đời của Đà Nẵng, hiện khai thác các sân bóng đá mini cho thuê theo giờ.', 'FACILITY', complex_id,
        '06:00', '22:00', 280000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        280000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE', 'FIVE_A_SIDE', 'SEVEN_A_SIDE'])[court_no],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Football')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Áo bib', 30000.00, 12, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng đá dự phòng', 50000.00, 6, TRUE FROM courts_0
)
SELECT count(*) FROM slots_final;

-- Sân vận động Thanh Khê (Đà Nẵng) — Football
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Thanh Khê',
        'Sân bóng đá phong trào khu vực quận Thanh Khê, mặt cỏ nhân tạo tiêu chuẩn.',
        'Đường Điện Biên Phủ, Thanh Khê, Đà Nẵng', NULL,
        16.0609768, 108.1831643,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Đà Nẵng', 'Thanh Khê'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Khu sân bóng đá', 'Sân bóng đá phong trào khu vực quận Thanh Khê, mặt cỏ nhân tạo tiêu chuẩn.', 'FACILITY', complex_id,
        '06:00', '22:00', 260000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        260000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE', 'SEVEN_A_SIDE'])[court_no],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Football')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Áo bib', 30000.00, 12, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng đá dự phòng', 50000.00, 6, TRUE FROM courts_0
)
SELECT count(*) FROM slots_final;

-- Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng (Đà Nẵng) — Tennis + Badminton
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng',
        'Cơ sở huấn luyện thể thao quốc gia, mở cửa cho thuê sân tennis và cầu lông ngoài giờ tập luyện.',
        'Đường Nguyễn Lương Bằng, Liên Chiểu, Đà Nẵng', NULL,
        16.0703699, 108.1793402,
        'https://images.pexels.com/photos/3845084/pexels-photo-3845084.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Đà Nẵng', 'Liên Chiểu'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007419/pexels-photo-8007419.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Khu sân tennis', 'Cơ sở huấn luyện thể thao quốc gia, mở cửa cho thuê sân tennis và cầu lông ngoài giờ tập luyện.', 'FACILITY', complex_id,
        '06:00', '22:00', 220000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        220000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Khu cầu lông', 'Cơ sở huấn luyện thể thao quốc gia, mở cửa cho thuê sân tennis và cầu lông ngoài giờ tập luyện.', 'FACILITY', complex_id,
        '06:00', '22:00', 75000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        75000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Tennis', 'Badminton')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Vợt tennis (thuê)', 50000.00, 8, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng tennis (hộp 3 quả)', 60000.00, 12, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Vợt cầu lông (thuê)', 30000.00, 10, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Cầu lông (ống 12 quả)', 85000.00, 15, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

-- Sân bóng rổ Quân Khu 5 (Đà Nẵng) — Basketball
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ Quân Khu 5',
        'Sân bóng rổ ngoài trời trong khuôn viên Quân khu 5, sàn nhựa tổng hợp.',
        'Đường Duy Tân, Hải Châu, Đà Nẵng', NULL,
        16.046809, 108.2159443,
        'https://images.pexels.com/photos/16599399/pexels-photo-16599399.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Đà Nẵng', 'Hải Châu'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8979885/pexels-photo-8979885.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30619256/pexels-photo-30619256.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Khu sân bóng rổ', 'Sân bóng rổ ngoài trời trong khuôn viên Quân khu 5, sàn nhựa tổng hợp.', 'FACILITY', complex_id,
        '06:00', '22:00', 170000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        170000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Basketball')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Bóng rổ (thuê)', 30000.00, 8, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Áo thi đấu', 25000.00, 10, TRUE FROM courts_0
)
SELECT count(*) FROM slots_final;

-- Sân vận động Hàng Đẫy (Hà Nội) — Football
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động Hàng Đẫy',
        'Sân vận động biểu tượng của bóng đá Hà Nội, khu vực phụ cận khai thác sân bóng mini cho thuê.',
        '9 Phố Trịnh Hoài Đức, Đống Đa, Hà Nội', NULL,
        21.0298699, 105.8329245,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hà Nội', 'Đống Đa'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Khu sân bóng đá', 'Sân vận động biểu tượng của bóng đá Hà Nội, khu vực phụ cận khai thác sân bóng mini cho thuê.', 'FACILITY', complex_id,
        '06:00', '22:00', 350000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        350000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE', 'SEVEN_A_SIDE', 'SEVEN_A_SIDE'])[court_no],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Football')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Áo bib', 30000.00, 12, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng đá dự phòng', 50000.00, 6, TRUE FROM courts_0
)
SELECT count(*) FROM slots_final;

-- Cung thể thao Quần Ngựa (Hà Nội) — Badminton + Basketball
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Cung thể thao Quần Ngựa',
        'Cung thể thao đa năng trung tâm Ba Đình, tổ chức nhiều giải đấu phong trào và cho thuê sân theo giờ.',
        '30 Đường Văn Cao, Ba Đình, Hà Nội', NULL,
        21.0402702, 105.8145946,
        'https://images.pexels.com/photos/8007493/pexels-photo-8007493.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hà Nội', 'Ba Đình'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/36774665/pexels-photo-36774665.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/9488949/pexels-photo-9488949.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Khu cầu lông', 'Cung thể thao đa năng trung tâm Ba Đình, tổ chức nhiều giải đấu phong trào và cho thuê sân theo giờ.', 'FACILITY', complex_id,
        '06:00', '22:00', 80000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        80000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Khu bóng rổ', 'Cung thể thao đa năng trung tâm Ba Đình, tổ chức nhiều giải đấu phong trào và cho thuê sân theo giờ.', 'FACILITY', complex_id,
        '06:00', '22:00', 180000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        180000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Badminton', 'Basketball')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Vợt cầu lông (thuê)', 30000.00, 10, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Cầu lông (ống 12 quả)', 85000.00, 15, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng rổ (thuê)', 30000.00, 8, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Áo thi đấu', 25000.00, 10, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

-- Nhà thi đấu Tương Mai (Hà Nội) — Basketball + Volleyball
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Nhà thi đấu Tương Mai',
        'Nhà thi đấu quận Hoàng Mai, phục vụ bóng rổ và bóng chuyền phong trào.',
        '5 Ngõ 104 Nguyễn An Ninh, Hoàng Mai, Hà Nội', NULL,
        20.9910576, 105.8460988,
        'https://images.pexels.com/photos/5586472/pexels-photo-5586472.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hà Nội', 'Hoàng Mai'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/10397639/pexels-photo-10397639.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/36382714/pexels-photo-36382714.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Khu bóng rổ', 'Nhà thi đấu quận Hoàng Mai, phục vụ bóng rổ và bóng chuyền phong trào.', 'FACILITY', complex_id,
        '06:00', '22:00', 165000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        165000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Khu bóng chuyền', 'Nhà thi đấu quận Hoàng Mai, phục vụ bóng rổ và bóng chuyền phong trào.', 'FACILITY', complex_id,
        '06:00', '22:00', 150000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        150000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 1) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Basketball', 'Volleyball')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Bóng rổ (thuê)', 30000.00, 8, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Áo thi đấu', 25000.00, 10, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng chuyền (thuê)', 30000.00, 6, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Lưới dự phòng', 100000.00, 2, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

-- Sân vận động Thống Nhất (Hồ Chí Minh) — Football
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Thống Nhất',
        'Sân vận động lịch sử của bóng đá TP.HCM, khu vực xung quanh khai thác sân bóng mini cho thuê.',
        '138 Đào Duy Từ, Quận 10, TP. Hồ Chí Minh', NULL,
        10.7607013, 106.6632761,
        'https://images.pexels.com/photos/33827014/pexels-photo-33827014.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hồ Chí Minh', 'Quận 10'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Khu sân bóng đá', 'Sân vận động lịch sử của bóng đá TP.HCM, khu vực xung quanh khai thác sân bóng mini cho thuê.', 'FACILITY', complex_id,
        '06:00', '22:00', 320000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        320000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE', 'FIVE_A_SIDE', 'SEVEN_A_SIDE'])[court_no],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Football')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Áo bib', 30000.00, 12, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng đá dự phòng', 50000.00, 6, TRUE FROM courts_0
)
SELECT count(*) FROM slots_final;

-- Nhà thi đấu đa năng Quận 7 (Hồ Chí Minh) — Badminton + Tennis
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Nhà thi đấu đa năng Quận 7',
        'Nhà thi đấu đa năng hiện đại Quận 7, có sân cầu lông và tennis đạt chuẩn.',
        '504 Đường Huỳnh Tấn Phát, Quận 7, TP. Hồ Chí Minh', NULL,
        10.7413684, 106.7292511,
        'https://images.pexels.com/photos/8286363/pexels-photo-8286363.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hồ Chí Minh', 'Quận 7'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007173/pexels-photo-8007173.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/38377403/pexels-photo-38377403.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Khu cầu lông', 'Nhà thi đấu đa năng hiện đại Quận 7, có sân cầu lông và tennis đạt chuẩn.', 'FACILITY', complex_id,
        '06:00', '22:00', 85000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        85000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 3) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Khu sân tennis', 'Nhà thi đấu đa năng hiện đại Quận 7, có sân cầu lông và tennis đạt chuẩn.', 'FACILITY', complex_id,
        '06:00', '22:00', 240000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        240000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Badminton', 'Tennis')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Vợt cầu lông (thuê)', 30000.00, 10, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Cầu lông (ống 12 quả)', 85000.00, 15, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Vợt tennis (thuê)', 50000.00, 8, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Bóng tennis (hộp 3 quả)', 60000.00, 12, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

-- Nhà thi đấu Quân khu 7 (Hồ Chí Minh) — Basketball + Volleyball
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at,
        province, district
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Nhà thi đấu Quân khu 7',
        'Nhà thi đấu Quân khu 7, phục vụ bóng rổ và bóng chuyền cho thuê theo giờ.',
        '202 Hoàng Văn Thụ, Phú Nhuận, TP. Hồ Chí Minh', NULL,
        10.8009258, 106.6688394,
        'https://images.pexels.com/photos/5275524/pexels-photo-5275524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 5.0, 0, NOW(),
        'Hồ Chí Minh', 'Phú Nhuận'
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8373608/pexels-photo-8373608.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203569/pexels-photo-6203569.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
facility_0 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Khu bóng rổ', 'Nhà thi đấu Quân khu 7, phục vụ bóng rổ và bóng chuyền cho thuê theo giờ.', 'FACILITY', complex_id,
        '06:00', '22:00', 175000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_0 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        175000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_0 f, generate_series(1, 2) AS court_no
    RETURNING stadium_id, price_per_hour
),
facility_1 AS (
    INSERT INTO stadiums (
        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Khu bóng chuyền', 'Nhà thi đấu Quân khu 7, phục vụ bóng rổ và bóng chuyền cho thuê theo giờ.', 'FACILITY', complex_id,
        '06:00', '22:00', 155000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
courts_1 AS (
    INSERT INTO stadiums (
        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        155000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM facility_1 f, generate_series(1, 1) AS court_no
    RETURNING stadium_id, price_per_hour
),
link_sport_types AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, sport_type_id FROM new_complex CROSS JOIN (
        SELECT sport_type_id FROM sport_types WHERE sport_name IN ('Basketball', 'Volleyball')
    ) st
    ON CONFLICT DO NOTHING
),
link_amenities AS (
    INSERT INTO complex_amenities (complex_id, amenity_id)
    SELECT complex_id, amenity_id FROM new_complex CROSS JOIN (
        SELECT amenity_id FROM amenities WHERE name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ')
    ) am
    ON CONFLICT DO NOTHING
),
all_courts AS (
    SELECT stadium_id, price_per_hour FROM courts_0
    UNION ALL
    SELECT stadium_id, price_per_hour FROM courts_1
),
slots_final AS (
    INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
    SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), price_per_hour, 'AVAILABLE'
    FROM all_courts, generate_series(6, 21) AS h
    RETURNING stadium_id
),
accessories_final AS (
    INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
    SELECT stadium_id, 'Bóng rổ (thuê)', 30000.00, 8, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Áo thi đấu', 25000.00, 10, TRUE FROM courts_0
    UNION ALL
    SELECT stadium_id, 'Bóng chuyền (thuê)', 30000.00, 6, TRUE FROM courts_1
    UNION ALL
    SELECT stadium_id, 'Lưới dự phòng', 100000.00, 2, TRUE FROM courts_1
)
SELECT count(*) FROM slots_final;

