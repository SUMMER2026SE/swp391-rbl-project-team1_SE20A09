-- ══════════════════════════════════════════════════════════════════════════
-- V7.6__seed_real_stadiums_osm.sql — Seed sân thật từ OpenStreetMap
-- Nguồn: scripts/fetch-overpass-stadiums.mjs (Overpass API, dữ liệu OSM/ODbL)
-- Sinh tự động bởi scripts/generate-seed-sql-from-osm.mjs — 161 địa điểm
-- Giá thuê/giờ, giờ mở cửa, rating là dữ liệu GIẢ LẬP (OSM không có) — chỉ
-- tên, địa chỉ, toạ độ, môn thể thao là thật.
-- Ảnh (cover_image_url, stadium_complex_images) là ảnh stock thật lấy từ
-- Pexels API (scripts/fetch-pexels-images.mjs) theo môn thể thao — KHÔNG
-- phải ảnh chụp đúng địa điểm cụ thể đó (không có nguồn hợp pháp cho việc này).
-- ══════════════════════════════════════════════════════════════════════════
-- Sân Tennis Vĩnh Tuy (Hà Nội) — osm:node/11644978543
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Tennis Vĩnh Tuy',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9983768, 105.8747737,
        'https://images.pexels.com/photos/36034890/pexels-photo-36034890.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.66, 91, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6010279/pexels-photo-6010279.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis Vĩnh Tuy', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 288000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        288000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 288000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng rổ (Hà Nội) — osm:node/11655877369
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Bóng rổ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0004356, 105.8446627,
        'https://images.pexels.com/photos/5274524/pexels-photo-5274524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.32, 58, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8693984/pexels-photo-8693984.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân Bóng rổ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 201000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        201000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 201000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng chuyền (Hà Nội) — osm:node/11655877370
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng chuyền',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0005019, 105.8439939,
        'https://images.pexels.com/photos/36382714/pexels-photo-36382714.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.87, 21, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203584/pexels-photo-6203584.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203569/pexels-photo-6203569.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân Bóng chuyền', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 136000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        136000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 136000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Cầu lông & Bóng bàn (Hà Nội) — osm:node/11655877371
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Cầu lông & Bóng bàn',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0007837, 105.8440279,
        'https://images.pexels.com/photos/8007075/pexels-photo-8007075.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.14, 36, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007173/pexels-photo-8007173.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Cầu lông & Bóng bàn', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 82000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        82000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 4) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 82000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Đại học Hà Nội (Hà Nội) — osm:way/37871558
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Đại học Hà Nội',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9895425, 105.795711,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.05, 9, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Đại học Hà Nội', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 372000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        372000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 372000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Tân Mai (Hà Nội) — osm:way/156006450
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Tân Mai',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.979882, 105.8507944,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.89, 54, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Tân Mai', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 477000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        477000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 477000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ Zone Six7 (Hà Nội) — osm:way/180002219
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ Zone Six7',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Ngõ 67 Thái Thịnh, Hà Nội',
        NULL,
        21.0086062, 105.8177244,
        'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.92, 3, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/5666179/pexels-photo-5666179.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30894524/pexels-photo-30894524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân bóng rổ Zone Six7', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 182000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        182000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 182000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Hàng Đẫy (Hà Nội) — osm:way/186664791
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động Hàng Đẫy',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '9, Phố Trịnh Hoài Đức, Đống Đa, Hà Nội',
        NULL,
        21.0298699, 105.8329245,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.90, 96, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Hàng Đẫy', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 397000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        397000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 397000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Quốc gia Mỹ Đình (Hà Nội) — osm:way/533838254
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Quốc gia Mỹ Đình',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '1, Đường Lê Đức Thọ, Quận Nam Từ Liêm, Hà Nội',
        NULL,
        21.0204982, 105.7639272,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.54, 76, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Quốc gia Mỹ Đình', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 528000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        528000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 528000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Quần Vợt (Hà Nội) — osm:way/591303965
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Quần Vợt',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9890932, 105.7985485,
        'https://images.pexels.com/photos/3845084/pexels-photo-3845084.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.53, 45, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30617588/pexels-photo-30617588.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Quần Vợt', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 253000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        253000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 253000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- sân bóng đá Hồng Hà (Hà Nội) — osm:way/593653781
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'sân bóng đá Hồng Hà',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0474567, 105.8442976,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.93, 3, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'sân bóng đá Hồng Hà', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 432000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        432000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 432000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- lina (Hà Nội) — osm:way/630481428
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'lina',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0177339, 105.7751826,
        'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.46, 27, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30617588/pexels-photo-30617588.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'lina', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 281000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        281000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 281000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Phường Cổ Nhuế 2 (Hà Nội) — osm:way/699057524
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Phường Cổ Nhuế 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0584097, 105.7793913,
        'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.63, 116, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Phường Cổ Nhuế 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 426000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        426000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 426000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động (Hà Nội) — osm:way/730958852
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9612909, 105.8277583,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.54, 54, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 585000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        585000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 585000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đại học Quốc gia Hà Nội (Hà Nội) — osm:way/736410377
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Đại học Quốc gia Hà Nội',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0399434, 105.7821918,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.41, 60, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đại học Quốc gia Hà Nội', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 564000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        564000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 564000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đầm Hồng 1 (Hà Nội) — osm:way/755360063
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Đầm Hồng 1',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9949689, 105.8232414,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.52, 60, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đầm Hồng 1', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 451000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        451000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 451000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ KTX Bách Khoa (Hà Nội) — osm:way/785554942
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ KTX Bách Khoa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0058324, 105.8461918,
        'https://images.pexels.com/photos/16599399/pexels-photo-16599399.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.69, 44, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/37143606/pexels-photo-37143606.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/35984242/pexels-photo-35984242.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ KTX Bách Khoa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 154000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        154000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 154000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Văn Quán (Hà Nội) — osm:way/785736479
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Văn Quán',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9737764, 105.7864465,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.73, 108, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Văn Quán', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 473000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        473000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 473000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Mật Mã (Hà Nội) — osm:way/788134000
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Mật Mã',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9812571, 105.7970208,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.16, 104, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Mật Mã', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 362000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        362000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 362000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng BACVIET (Hà Nội) — osm:way/788138972
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng BACVIET',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9731564, 105.7933136,
        'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.16, 26, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng BACVIET', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 551000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        551000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 551000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Cầu Đơ (Hà Nội) — osm:way/788143845
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Cầu Đơ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9685325, 105.7766846,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.74, 118, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Cầu Đơ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 532000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        532000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 532000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Phúc La (Hà Nội) — osm:way/788145290
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Phúc La',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9695306, 105.7872813,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.78, 90, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Phúc La', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 472000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        472000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 472000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Mộ Lao (Hà Nội) — osm:way/788152174
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Mộ Lao',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9811476, 105.7825995,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.90, 10, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Mộ Lao', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 383000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        383000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 383000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Zone9 (Hà Nội) — osm:way/789339479
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Zone9',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9721716, 105.7934397,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.07, 108, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Zone9', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 351000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        351000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 351000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Bưu Chính (Hà Nội) — osm:way/789731736
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Bưu Chính',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9806281, 105.7867231,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.13, 7, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Bưu Chính', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 533000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        533000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 533000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Vạn Phúc (Hà Nội) — osm:way/789734282
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Vạn Phúc',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9790639, 105.7789343,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.24, 15, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Vạn Phúc', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 527000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        527000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 527000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Giáp Nhất (Hà Nội) — osm:way/823022224
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Giáp Nhất',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0047359, 105.8138885,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.07, 21, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Giáp Nhất', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 567000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        567000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 567000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng HVNH (Hà Nội) — osm:way/826025421
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng HVNH',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0097028, 105.8299115,
        'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.69, 35, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng HVNH', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 600000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        600000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 600000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đại học Quốc gia Hà Nội (Hà Nội) — osm:way/828164289
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Đại học Quốc gia Hà Nội',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0399352, 105.7824396,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.44, 113, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đại học Quốc gia Hà Nội', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 531000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        531000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 531000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Thái Hà (Hà Nội) — osm:way/839881664
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Thái Hà',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0154712, 105.8193424,
        'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.64, 107, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Thái Hà', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 480000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        480000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 480000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng The Garden (Hà Nội) — osm:way/891828626
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng The Garden',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9749798, 105.8805128,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.66, 34, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng The Garden', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 440000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        440000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 440000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Yên Hòa (Hà Nội) — osm:way/893386369
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Yên Hòa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0233223, 105.7914136,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.54, 85, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Yên Hòa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 393000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        393000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 393000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Bách Khoa (Hà Nội) — osm:way/947595779
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Bách Khoa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0022011, 105.8478568,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.10, 104, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Bách Khoa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 315000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        315000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 315000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- sân bóng 8 (Hà Nội) — osm:way/949406906
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'sân bóng 8',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.074979, 105.7862006,
        'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.81, 58, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'sân bóng 8', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 485000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        485000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 485000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- san bong 12 (Hà Nội) — osm:way/949562970
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'san bong 12',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0727689, 105.7865671,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.80, 45, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'san bong 12', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 411000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        411000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 411000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân B5 (Hà Nội) — osm:way/958560272
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân B5',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9808305, 105.7865746,
        'https://images.pexels.com/photos/6203532/pexels-photo-6203532.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.79, 43, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203575/pexels-photo-6203575.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203533/pexels-photo-6203533.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân B5', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 134000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        134000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 134000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Trung Kính Hạ (Hà Nội) — osm:way/965813005
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Trung Kính Hạ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Phố Tú Mỡ, Hà Nội',
        NULL,
        21.0117939, 105.7955282,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.81, 17, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Trung Kính Hạ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 488000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        488000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 488000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đại Từ Đặng Xuân Bảng (Hà Nội) — osm:way/1002478596
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Đại Từ Đặng Xuân Bảng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9741163, 105.8275995,
        'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.82, 0, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đại Từ Đặng Xuân Bảng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 310000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        310000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 310000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Nhà thi đấu Tương Mai (Hà Nội) — osm:way/1022824342
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Nhà thi đấu Tương Mai',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '5, Ngõ 104 Nguyễn An Ninh, Hà Nội',
        NULL,
        20.9910576, 105.8460988,
        'https://images.pexels.com/photos/8693557/pexels-photo-8693557.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.11, 119, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/35984242/pexels-photo-35984242.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Nhà thi đấu Tương Mai', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 205000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        205000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 205000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá 99 Định Công (Hà Nội) — osm:way/1043760468
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá 99 Định Công',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9813678, 105.8276689,
        'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.80, 77, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá 99 Định Công', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 392000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        392000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 392000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Trung tâm Văn hóa - Thông tin và Thể thao Đống Đa (Hà Nội) — osm:way/1082814013
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Trung tâm Văn hóa - Thông tin và Thể thao Đống Đa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '42, Phố Hoàng Cầu, Đống Đa, Hà Nội',
        '+84 243 857 2726',
        21.0154145, 105.8191422,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.75, 20, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Trung tâm Văn hóa - Thông tin và Thể thao Đống Đa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 600000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        600000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 600000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Nhật Minh (Hà Nội) — osm:way/1104873485
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Nhật Minh',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '32, Phố Linh Đường, Hà Nội',
        NULL,
        20.9578126, 105.833977,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.83, 113, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Nhật Minh', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 393000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        393000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 393000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Thông tin (Hà Nội) — osm:way/1107422065
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Thông tin',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9831661, 105.8251839,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.04, 47, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Thông tin', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 421000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        421000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 421000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng chuyền hồ Đền Lừ (Hà Nội) — osm:way/1107618522
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng chuyền hồ Đền Lừ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9843249, 105.8570206,
        'https://images.pexels.com/photos/35887582/pexels-photo-35887582.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.27, 26, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203569/pexels-photo-6203569.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203563/pexels-photo-6203563.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân bóng chuyền hồ Đền Lừ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 110000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        110000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 110000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Huy Hoàng - Gamuda (Hà Nội) — osm:way/1141484374
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Huy Hoàng - Gamuda',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9745652, 105.8810135,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.80, 111, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Huy Hoàng - Gamuda', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 479000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        479000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 479000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng La Thành (Hà Nội) — osm:way/1213887677
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng La Thành',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9916596, 105.82607,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.06, 86, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng La Thành', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 350000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        350000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 350000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Câu lạc bộ Thể thao Khuyết tật Hà Nội (Hà Nội) — osm:way/1216036363
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Câu lạc bộ Thể thao Khuyết tật Hà Nội',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '1B, Phố Lê Hồng Phong, Phường Ba Đình, Hà Nội',
        NULL,
        21.0331976, 105.8378186,
        'https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.77, 19, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6010279/pexels-photo-6010279.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Câu lạc bộ Thể thao Khuyết tật Hà Nội', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 236000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        236000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 236000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Thong Nhat Football Field (Hà Nội) — osm:way/1216973612
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Thong Nhat Football Field',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0122788, 105.8991252,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.36, 53, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Thong Nhat Football Field', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 588000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        588000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 588000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Thành Lâm 3 (Hà Nội) — osm:way/1217694015
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Thành Lâm 3',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Ngõ 3 Tôn Thất Thuyết, Cầu Giấy, Hà Nội',
        NULL,
        21.026535, 105.7852859,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.73, 82, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Thành Lâm 3', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 429000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        429000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 429000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Trung tâm Văn hóa - Thông tin và Thể thao phường Giảng Võ (Hà Nội) — osm:way/1217754305
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Trung tâm Văn hóa - Thông tin và Thể thao phường Giảng Võ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '5A, Phố Thành Công, Hà Nội',
        NULL,
        21.0201132, 105.8146546,
        'https://images.pexels.com/photos/8783116/pexels-photo-8783116.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.52, 92, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8979885/pexels-photo-8979885.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/16599399/pexels-photo-16599399.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Trung tâm Văn hóa - Thông tin và Thể thao phường Giảng Võ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 232000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        232000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 232000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Ven Đê 3 (Hà Nội) — osm:way/1250795271
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Ven Đê 3',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0297471, 105.8713054,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.37, 94, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Ven Đê 3', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 391000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        391000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 391000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Ven Đê 2 (Hà Nội) — osm:way/1250795285
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Bóng Ven Đê 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0306572, 105.8727501,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.55, 109, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Ven Đê 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 445000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        445000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 445000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Trung tâm huấn luyện Vận động viên (Hà Nội) — osm:way/1305506358
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Trung tâm huấn luyện Vận động viên',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.029618, 105.8318298,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.31, 31, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Trung tâm huấn luyện Vận động viên', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 518000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        518000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 518000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Văn phòng Quốc hội (Hà Nội) — osm:way/1323092578
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Văn phòng Quốc hội',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0326405, 105.7536986,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.96, 19, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Văn phòng Quốc hội', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 361000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        361000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 361000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Mễ Trì Park의 종합 운동장 (Hà Nội) — osm:way/1337572246
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Mễ Trì Park의 종합 운동장',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0065406, 105.7727079,
        'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.60, 3, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26238655/pexels-photo-26238655.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007493/pexels-photo-8007493.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Mễ Trì Park의 종합 운동장', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 73000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        73000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 73000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Rổ (Hà Nội) — osm:way/1350647803
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Bóng Rổ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0170123, 105.8006103,
        'https://images.pexels.com/photos/8783116/pexels-photo-8783116.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.87, 43, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8979885/pexels-photo-8979885.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân Bóng Rổ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 268000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        268000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 268000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng chuyền (Hà Nội) — osm:way/1359824053
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng chuyền',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.0224054, 105.8065652,
        'https://images.pexels.com/photos/35887582/pexels-photo-35887582.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.59, 116, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203569/pexels-photo-6203569.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203584/pexels-photo-6203584.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân bóng chuyền', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 145000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        145000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 145000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Hồng Quang (Hà Nội) — osm:way/1435347674
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Hồng Quang',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9736527, 105.8319145,
        'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.87, 33, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Hồng Quang', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 598000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        598000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 598000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Đền Lừ 3 (Hà Nội) — osm:way/1444628959
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng Đền Lừ 3',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9829754, 105.8547282,
        'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.16, 21, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Đền Lừ 3', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 574000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        574000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 574000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá CLB Thể thao Thiếu nhi Tân Mai (Hà Nội) — osm:way/1445339894
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá CLB Thể thao Thiếu nhi Tân Mai',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9849096, 105.8486726,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.40, 51, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá CLB Thể thao Thiếu nhi Tân Mai', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 335000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        335000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 335000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đại Nam (Hà Nội) — osm:way/1449575821
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Đại Nam',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.968646, 105.802856,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.76, 28, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đại Nam', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 582000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        582000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 582000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Văn La (Hà Nội) — osm:way/1449590959
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Văn La',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9549041, 105.7637715,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.67, 61, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Văn La', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 503000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        503000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 503000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Xoài Arena - Tổ hợp sân Cầu Lông (Hà Nội) — osm:way/1450359245
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Xoài Arena - Tổ hợp sân Cầu Lông',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.051603, 105.8814885,
        'https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.42, 59, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007173/pexels-photo-8007173.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007094/pexels-photo-8007094.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Xoài Arena - Tổ hợp sân Cầu Lông', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 73000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        73000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 73000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Kho (Hà Nội) — osm:way/1452309109
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Kho',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9938095, 105.8461755,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.44, 104, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Kho', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 403000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        403000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 403000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Đá HLP - Đồng Phát (Hà Nội) — osm:way/1454185280
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng Đá HLP - Đồng Phát',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9809594, 105.869465,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.75, 8, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Đá HLP - Đồng Phát', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 510000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        510000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 510000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- SixtyNine Corner (Hà Nội) — osm:way/1456814209
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'SixtyNine Corner',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Ngõ 89 Phố Tứ Liên, Hà Nội',
        NULL,
        21.0689662, 105.8326149,
        'https://images.pexels.com/photos/5274524/pexels-photo-5274524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.86, 11, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/35984242/pexels-photo-35984242.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/16599399/pexels-photo-16599399.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'SixtyNine Corner', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 176000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        176000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 176000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng AKKA Vĩnh Hoàng (Hà Nội) — osm:way/1460756541
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng AKKA Vĩnh Hoàng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9807574, 105.8683869,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.86, 0, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng AKKA Vĩnh Hoàng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 419000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        419000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 419000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng aladdin (Hà Nội) — osm:way/1464406217
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng aladdin',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.075547, 105.8331027,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.16, 81, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng aladdin', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 581000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        581000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 581000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Quân đội Hoàng Mai (Hà Nội) — osm:way/1468719070
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Quân đội Hoàng Mai',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9875243, 105.8602804,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.97, 24, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Quân đội Hoàng Mai', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 399000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        399000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 399000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Tin Lớn (Hà Nội) — osm:way/1473543487
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Tin Lớn',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        21.028581, 105.8738438,
        'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.86, 93, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Tin Lớn', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 328000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        328000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 328000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Đầm Hồng 2 (Hà Nội) — osm:way/1478738677
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Đầm Hồng 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9905526, 105.8250253,
        'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.30, 119, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Đầm Hồng 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 562000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        562000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 562000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Định Công 2 (Hà Nội) — osm:way/1479282622
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Định Công 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9755773, 105.8272282,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.52, 62, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Định Công 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 551000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        551000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 551000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Hoa Mừng (Hà Nội) — osm:way/1481981836
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Hoa Mừng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9800479, 105.838861,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.87, 102, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Hoa Mừng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 351000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        351000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 351000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis-Pickleball Đại Kim (Hà Nội) — osm:way/1484017978
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Tennis-Pickleball Đại Kim',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9776264, 105.836946,
        'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.88, 40, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30617588/pexels-photo-30617588.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6931243/pexels-photo-6931243.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis-Pickleball Đại Kim', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 293000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        293000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 293000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng BCH (Hà Nội) — osm:way/1490065649
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng BCH',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9603972, 105.8650004,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.65, 73, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng BCH', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 468000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        468000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 468000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Bảo Lâm (Hà Nội) — osm:way/1493788008
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Bảo Lâm',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9824704, 105.8746642,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.22, 55, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Bảo Lâm', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 487000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        487000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 487000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Thành Nam (Hà Nội) — osm:way/1493788053
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Thành Nam',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9801656, 105.8814925,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.63, 67, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Thành Nam', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 311000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        311000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 311000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân cầu lông công viên Vĩnh Hoàng (Hà Nội) — osm:way/1511491222
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân cầu lông công viên Vĩnh Hoàng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hà Nội',
        NULL,
        20.9941367, 105.875442,
        'https://images.pexels.com/photos/8286363/pexels-photo-8286363.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.55, 61, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Sân cầu lông công viên Vĩnh Hoàng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 73000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        73000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 4) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 73000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Cẩm Lệ (Đà Nẵng) — osm:node/3762853481
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Cẩm Lệ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Trường Sơn, Cẩm Lệ, Đà Nẵng',
        NULL,
        16.0148875, 108.1891412,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.62, 33, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Cẩm Lệ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 444000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        444000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 444000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Tiểu La (Đà Nẵng) — osm:way/295856685
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Tiểu La',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        16.04498, 108.2129961,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.94, 99, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Tiểu La', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 331000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        331000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 331000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Thanh Khê (Đà Nẵng) — osm:way/645923001
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Thanh Khê',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        16.0609768, 108.1831643,
        'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.87, 92, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Thanh Khê', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 562000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        562000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 562000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- San Tennis (Đà Nẵng) — osm:way/645923004
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'San Tennis',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        15.976461, 108.2515296,
        'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.89, 18, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/9739462/pexels-photo-9739462.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6931243/pexels-photo-6931243.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'San Tennis', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 247000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        247000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 247000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis khu Vui chơi Phần Lăng (Đà Nẵng) — osm:way/649959737
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Tennis khu Vui chơi Phần Lăng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        16.062626, 108.183059,
        'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.78, 110, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/36034890/pexels-photo-36034890.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis khu Vui chơi Phần Lăng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 261000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        261000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 261000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ Quân Khu 5 (Đà Nẵng) — osm:way/727113498
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng rổ Quân Khu 5',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        16.046809, 108.2159443,
        'https://images.pexels.com/photos/37143606/pexels-photo-37143606.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.35, 68, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/5586472/pexels-photo-5586472.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ Quân Khu 5', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 165000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        165000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 165000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Chi Lăng (Đà Nẵng) — osm:way/727205239
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Chi Lăng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đà Nẵng',
        NULL,
        15.9992598, 108.2233504,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.73, 65, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Chi Lăng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 326000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        326000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 326000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Mini Nghĩa Thảo (Hồ Chí Minh) — osm:node/1628403128
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Bóng Mini Nghĩa Thảo',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7110943, 106.742877,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.99, 7, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Mini Nghĩa Thảo', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 417000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        417000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 417000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Đá Mini Bình Trị (Hồ Chí Minh) — osm:node/6538599359
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng Đá Mini Bình Trị',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Chiến Lược, Bình Tân, Hồ Chí Minh',
        NULL,
        10.7605388, 106.6092141,
        'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.93, 36, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Đá Mini Bình Trị', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 379000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        379000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 379000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Câu Lạc Bộ Cầu Lông T793 (Hồ Chí Minh) — osm:node/9586881865
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Câu Lạc Bộ Cầu Lông T793',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7471263, 106.6985988,
        'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.90, 52, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/31791127/pexels-photo-31791127.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4931355/pexels-photo-4931355.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Câu Lạc Bộ Cầu Lông T793', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 76000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        76000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 4) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 76000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng đá & Bóng rổ Empire City (Hồ Chí Minh) — osm:node/10059435795
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng đá & Bóng rổ Empire City',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7681019, 106.7120952,
        'https://images.pexels.com/photos/8980690/pexels-photo-8980690.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.87, 33, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8693984/pexels-photo-8693984.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30619256/pexels-photo-30619256.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân Bóng đá & Bóng rổ Empire City', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 257000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        257000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 257000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- CLB Cầu Lông Hoàng Văn Thụ (Hồ Chí Minh) — osm:node/11754636541
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'CLB Cầu Lông Hoàng Văn Thụ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '202B, Hoàng Văn Thụ, Phú Nhuận, Ho Chi Minh City',
        NULL,
        10.8002755, 106.6700708,
        'https://images.pexels.com/photos/8007075/pexels-photo-8007075.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.68, 0, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26238656/pexels-photo-26238656.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/16688911/pexels-photo-16688911.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'CLB Cầu Lông Hoàng Văn Thụ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 117000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        117000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 4) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 117000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Thanh Niên 2 (Hồ Chí Minh) — osm:node/12107200007
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng Thanh Niên 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8539822, 106.6736209,
        'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.87, 39, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Thanh Niên 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 389000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        389000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 389000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Cầu Lông The B Hương Lộ 2 (Hồ Chí Minh) — osm:node/12418249325
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Cầu Lông The B Hương Lộ 2',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '859, Hương Lộ 2, Bình Tân, Hồ Chí Minh',
        NULL,
        10.7664071, 106.6000565,
        'https://images.pexels.com/photos/8007173/pexels-photo-8007173.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.81, 28, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26238655/pexels-photo-26238655.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Sân Cầu Lông The B Hương Lộ 2', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 113000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        113000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 113000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Outdoor badminton court (Hồ Chí Minh) — osm:node/12496314223
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Outdoor badminton court',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8270363, 106.7018339,
        'https://images.pexels.com/photos/16688911/pexels-photo-16688911.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.51, 29, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4931355/pexels-photo-4931355.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007419/pexels-photo-8007419.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Outdoor badminton court', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 73000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        73000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 73000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng (Hồ Chí Minh) — osm:node/13503266356
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7726081, 106.6593159,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.45, 108, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 481000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        481000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 481000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Rạch Cát Badminton Club (Hồ Chí Minh) — osm:node/13579698304
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Rạch Cát Badminton Club',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7148856, 106.626184,
        'https://images.pexels.com/photos/8286363/pexels-photo-8286363.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.64, 70, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007075/pexels-photo-8007075.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32944292/pexels-photo-32944292.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Rạch Cát Badminton Club', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 83000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        83000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 83000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis (Hồ Chí Minh) — osm:way/112337549
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Tennis',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7982847, 106.7407192,
        'https://images.pexels.com/photos/6010282/pexels-photo-6010282.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.41, 90, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4582494/pexels-photo-4582494.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 187000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        187000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 187000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Phú Mỹ Q7 (sân cát) (Hồ Chí Minh) — osm:way/149904640
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Phú Mỹ Q7 (sân cát)',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7122932, 106.7403758,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.86, 31, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Phú Mỹ Q7 (sân cát)', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 405000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        405000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 405000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Hoa Lư (Hồ Chí Minh) — osm:way/165567733
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động Hoa Lư',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7879111, 106.7016179,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.93, 81, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Hoa Lư', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 508000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        508000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 508000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Tao Đàn (Hồ Chí Minh) — osm:way/186276884
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Tao Đàn',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7752098, 106.6947592,
        'https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.14, 71, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Tao Đàn', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 560000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        560000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 560000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- CLB Quần Vợt Viettel (Hồ Chí Minh) — osm:way/186584463
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'CLB Quần Vợt Viettel',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8078004, 106.6491171,
        'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.69, 12, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6010279/pexels-photo-6010279.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4582494/pexels-photo-4582494.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'CLB Quần Vợt Viettel', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 252000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        252000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 252000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Hoàng Hoa Thám (Hồ Chí Minh) — osm:way/197983678
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Hoàng Hoa Thám',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.805219, 106.6485883,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.78, 63, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Hoàng Hoa Thám', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 502000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        502000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 502000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Đá Chảo Lửa (Hồ Chí Minh) — osm:way/242304823
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Bóng Đá Chảo Lửa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8059337, 106.6584014,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.73, 69, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Đá Chảo Lửa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 306000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        306000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 306000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân banh Cây Đa (Hồ Chí Minh) — osm:way/263171587
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân banh Cây Đa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8630238, 106.618905,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.55, 3, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân banh Cây Đa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 556000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        556000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 556000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Thuần Việt (Hồ Chí Minh) — osm:way/290197604
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Thuần Việt',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đường số 1A, Hồ Chí Minh',
        NULL,
        10.7352556, 106.6936788,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.08, 77, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Thuần Việt', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 588000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        588000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 588000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động (Hồ Chí Minh) — osm:way/290218569
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7326155, 106.6977518,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.51, 82, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 499000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        499000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 499000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân banh Phường Bình Đường (Hồ Chí Minh) — osm:way/291853358
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân banh Phường Bình Đường',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8676011, 106.7488298,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.05, 49, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân banh Phường Bình Đường', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 367000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        367000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 367000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Tiến Phát (Hồ Chí Minh) — osm:way/306183112
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Tiến Phát',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8349791, 106.7733979,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.65, 62, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Tiến Phát', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 456000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        456000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 456000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Vận Động (Hồ Chí Minh) — osm:way/317486481
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Vận Động',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.856552, 106.7589935,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.57, 53, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Vận Động', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 461000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        461000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 461000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân cỏ nhân tạo Thiên Trường (Hồ Chí Minh) — osm:way/358132326
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân cỏ nhân tạo Thiên Trường',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8093921, 106.7172565,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.91, 96, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân cỏ nhân tạo Thiên Trường', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 356000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        356000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 356000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân 320 (Hồ Chí Minh) — osm:way/359260947
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân 320',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.81882, 106.63904,
        'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.73, 14, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân 320', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 327000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        327000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 327000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân quần vợt Thanh Lam (Hồ Chí Minh) — osm:way/516774840
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân quần vợt Thanh Lam',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7493457, 106.6176579,
        'https://images.pexels.com/photos/6010279/pexels-photo-6010279.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.84, 59, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30894524/pexels-photo-30894524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân quần vợt Thanh Lam', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 247000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        247000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 247000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Lan Anh (Hồ Chí Minh) — osm:way/585932872
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Lan Anh',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7709562, 106.6712581,
        'https://images.pexels.com/photos/5739120/pexels-photo-5739120.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.24, 65, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6931243/pexels-photo-6931243.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Lan Anh', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 282000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        282000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 282000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Lan Anh (Hồ Chí Minh) — osm:way/585932873
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Lan Anh',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7710333, 106.6714143,
        'https://images.pexels.com/photos/6010282/pexels-photo-6010282.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.87, 88, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13425628/pexels-photo-13425628.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Lan Anh', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 151000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        151000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 151000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân banh số 8 (Hồ Chí Minh) — osm:way/597499978
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân banh số 8',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '29, Đường số 8, Bình Thạnh, Ho Chi Minh City',
        '+84 28 6673 5953',
        10.8129675, 106.7063269,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.66, 111, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân banh số 8', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 466000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        466000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 466000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Thống Nhất (Hồ Chí Minh) — osm:way/665450065
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân vận động Thống Nhất',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '138, Đào Duy Từ, Hồ Chí Minh',
        NULL,
        10.7607013, 106.6632761,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.92, 34, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Thống Nhất', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 495000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        495000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 495000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- sân bóng đá (Hồ Chí Minh) — osm:way/675570982
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'sân bóng đá',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.884076, 106.7760982,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.99, 83, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'sân bóng đá', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 435000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        435000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 435000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá mini Thăng Long (Hồ Chí Minh) — osm:way/714534691
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá mini Thăng Long',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đường số 4, Bình Tân, Hồ Chí Minh',
        NULL,
        10.7558165, 106.6070429,
        'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.96, 45, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá mini Thăng Long', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 352000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        352000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 352000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- sân bóng đường số 4 (Hồ Chí Minh) — osm:way/714543829
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'sân bóng đường số 4',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'đường số 4, Bình Tân, Hồ Chí Minh',
        NULL,
        10.7573248, 106.6072211,
        'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.60, 40, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'sân bóng đường số 4', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 381000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        381000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 381000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Bóng Đá Mini Nguyễn Hữu Đan (Hồ Chí Minh) — osm:way/716762886
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Bóng Đá Mini Nguyễn Hữu Đan',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7446349, 106.6107209,
        'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.04, 81, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Bóng Đá Mini Nguyễn Hữu Đan', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 500000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        500000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 500000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân vận động Quân khu 7 (Hồ Chí Minh) — osm:way/750361871
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân vận động Quân khu 7',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '202, Hoàng Văn Thụ, Tân Bình, Thành phố Hồ Chí Minh',
        NULL,
        10.8018529, 106.6672646,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.94, 106, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân vận động Quân khu 7', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 352000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        352000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 352000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Nhà thi đấu quận 12 (Hồ Chí Minh) — osm:way/992658930
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Nhà thi đấu quận 12',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        '+84396916078',
        10.8738062, 106.6384929,
        'https://images.pexels.com/photos/8007173/pexels-photo-8007173.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.88, 32, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4931355/pexels-photo-4931355.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Nhà thi đấu quận 12', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 70000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        70000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 70000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân tennis (Hồ Chí Minh) — osm:way/994918130
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân tennis',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7323834, 106.6971511,
        'https://images.pexels.com/photos/5739120/pexels-photo-5739120.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.62, 89, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/36034890/pexels-photo-36034890.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân tennis', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 272000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        272000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 272000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Phan Chu Trinh (Hồ Chí Minh) — osm:way/1036206949
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Phan Chu Trinh',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8151134, 106.7013745,
        'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.41, 76, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Phan Chu Trinh', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 344000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        344000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 344000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá HCA (Hồ Chí Minh) — osm:way/1036206953
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá HCA',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Chu Văn An, Bình Thạnh, Phường 12',
        NULL,
        10.8121888, 106.7032281,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.81, 73, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá HCA', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 364000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        364000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 364000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Mega Ruby Khang Điền Tennis Court (Hồ Chí Minh) — osm:way/1136988288
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Mega Ruby Khang Điền Tennis Court',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8003395, 106.7943707,
        'https://images.pexels.com/photos/5666179/pexels-photo-5666179.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.66, 64, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/36034890/pexels-photo-36034890.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/5739120/pexels-photo-5739120.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Mega Ruby Khang Điền Tennis Court', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 261000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        261000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 261000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- sân bóng (Hồ Chí Minh) — osm:way/1144380341
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'sân bóng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8884705, 106.7640464,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.33, 17, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'sân bóng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 545000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        545000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 545000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ (Hồ Chí Minh) — osm:way/1154279486
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.732373, 106.696968,
        'https://images.pexels.com/photos/30619256/pexels-photo-30619256.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.69, 81, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/37143606/pexels-photo-37143606.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8783116/pexels-photo-8783116.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 256000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        256000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 256000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ (Hồ Chí Minh) — osm:way/1156525356
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng rổ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.723607, 106.724392,
        'https://images.pexels.com/photos/8693557/pexels-photo-8693557.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.22, 110, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8979885/pexels-photo-8979885.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/33696837/pexels-photo-33696837.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 184000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        184000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 184000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân tennis (Hồ Chí Minh) — osm:way/1156525358
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân tennis',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7230933, 106.7241133,
        'https://images.pexels.com/photos/13425628/pexels-photo-13425628.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.35, 72, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/3845084/pexels-photo-3845084.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân tennis', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 189000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        189000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 189000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Sport Plus WeSport (Hồ Chí Minh) — osm:way/1162776052
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá Sport Plus WeSport',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8049534, 106.6122377,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.55, 99, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Sport Plus WeSport', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 488000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        488000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 488000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Mã Lò (Hồ Chí Minh) — osm:way/1211762401
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Mã Lò',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7844559, 106.5971502,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.58, 5, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Mã Lò', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 359000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        359000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 359000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân quần vợt TANIMEX (Hồ Chí Minh) — osm:way/1215108851
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân quần vợt TANIMEX',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8168558, 106.6087079,
        'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.62, 89, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/5666179/pexels-photo-5666179.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân quần vợt TANIMEX', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 157000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        157000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 157000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá mini Tân Thới Hòa (Hồ Chí Minh) — osm:way/1215418779
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá mini Tân Thới Hòa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hẻm 17 Lương Minh Nguyệt, Tân Phú, Phường Tân Thới Hòa',
        NULL,
        10.7601872, 106.6306613,
        'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.46, 82, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá mini Tân Thới Hòa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 390000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        390000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 390000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Khu thể thao Đảo Kim Cương (Hồ Chí Minh) — osm:way/1215518830
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Khu thể thao Đảo Kim Cương',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Thủ Đức, Phường Bình Trưng Tây',
        NULL,
        10.7786368, 106.7480962,
        'https://images.pexels.com/photos/30894524/pexels-photo-30894524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.56, 62, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30617588/pexels-photo-30617588.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6931243/pexels-photo-6931243.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Khu thể thao Đảo Kim Cương', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 175000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        175000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 175000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Chảo Lửa (Hồ Chí Minh) — osm:way/1219278791
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Chảo Lửa',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8066361, 106.6556877,
        'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.72, 30, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Chảo Lửa', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 521000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        521000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 521000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng chuyền khu phố 3 (Hồ Chí Minh) — osm:way/1221493475
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng chuyền khu phố 3',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8217351, 106.7193542,
        'https://images.pexels.com/photos/6203584/pexels-photo-6203584.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.79, 90, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203583/pexels-photo-6203583.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/36382714/pexels-photo-36382714.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân bóng chuyền khu phố 3', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 143000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        143000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 143000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá 13C (Hồ Chí Minh) — osm:way/1223689088
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá 13C',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.715664, 106.6427684,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.68, 44, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá 13C', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 416000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        416000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 416000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Câu lạc bộ Văn hóa Thể thao Tanimex (Hồ Chí Minh) — osm:way/1239928935
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Câu lạc bộ Văn hóa Thể thao Tanimex',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Đường CN13, Tân Phú, Thành phố Hồ Chí Minh',
        NULL,
        10.8084826, 106.6114908,
        'https://images.pexels.com/photos/36127007/pexels-photo-36127007.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.62, 11, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6010282/pexels-photo-6010282.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Câu lạc bộ Văn hóa Thể thao Tanimex', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 235000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        235000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 235000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis Khu Nam Long Q9 (Hồ Chí Minh) — osm:way/1271043480
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Tennis Khu Nam Long Q9',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8049786, 106.7795937,
        'https://images.pexels.com/photos/5739120/pexels-photo-5739120.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.23, 84, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6010279/pexels-photo-6010279.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis Khu Nam Long Q9', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 297000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        297000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 297000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Cao đẳng Saigon Tech (Hồ Chí Minh) — osm:way/1292050355
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng Cao đẳng Saigon Tech',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8558048, 106.6305917,
        'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.93, 33, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Cao đẳng Saigon Tech', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 596000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        596000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 596000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá NTĐ Quận 7 (Hồ Chí Minh) — osm:way/1308105274
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá NTĐ Quận 7',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7409141, 106.7290608,
        'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.18, 48, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá NTĐ Quận 7', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 537000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        537000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 537000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng chuyền ngoài trời NTĐ Quận 7 (Hồ Chí Minh) — osm:way/1308105275
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng chuyền ngoài trời NTĐ Quận 7',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7412496, 106.7289678,
        'https://images.pexels.com/photos/36382714/pexels-photo-36382714.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.79, 48, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/6203563/pexels-photo-6203563.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6203632/pexels-photo-6203632.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
        'Sân bóng chuyền ngoài trời NTĐ Quận 7', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 144000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        144000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 144000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ ngoài trời NTĐ Quận 7 (Hồ Chí Minh) — osm:way/1308105276
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ ngoài trời NTĐ Quận 7',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7418986, 106.7293404,
        'https://images.pexels.com/photos/16599399/pexels-photo-16599399.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.92, 21, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8693984/pexels-photo-8693984.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/5586472/pexels-photo-5586472.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ ngoài trời NTĐ Quận 7', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 253000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        253000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 253000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- RMIT University Football Pitch (Hồ Chí Minh) — osm:way/1323498030
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'RMIT University Football Pitch',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7297395, 106.6916471,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.91, 45, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'RMIT University Football Pitch', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 515000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        515000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 515000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Banh Đại Học Cảnh Sát (Hồ Chí Minh) — osm:way/1323498075
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Banh Đại Học Cảnh Sát',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7350765, 106.6958215,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.88, 67, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/27669822/pexels-photo-27669822.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân Banh Đại Học Cảnh Sát', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 523000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        523000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 523000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân cầu lông (Hồ Chí Minh) — osm:way/1331546372
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân cầu lông',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7930768, 106.7739263,
        'https://images.pexels.com/photos/32944292/pexels-photo-32944292.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.01, 13, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007094/pexels-photo-8007094.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Sân cầu lông', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 64000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        64000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 64000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ (Hồ Chí Minh) — osm:way/1331547426
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng rổ',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7925802, 106.7714126,
        'https://images.pexels.com/photos/30619256/pexels-photo-30619256.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.32, 32, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/9739466/pexels-photo-9739466.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8980690/pexels-photo-8980690.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 174000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        174000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 174000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân thể thao UEL (Hồ Chí Minh) — osm:way/1338236842
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân thể thao UEL',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8726028, 106.777544,
        'https://images.pexels.com/photos/12201296/pexels-photo-12201296.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.08, 68, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/17160683/pexels-photo-17160683.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân thể thao UEL', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 334000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        334000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 334000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Nhà thể thao UEL (Hồ Chí Minh) — osm:way/1338236843
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Nhà thể thao UEL',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8727344, 106.7779004,
        'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.56, 63, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007075/pexels-photo-8007075.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Nhà thể thao UEL', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 68000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        68000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 3) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 68000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng rổ UEL (Hồ Chí Minh) — osm:way/1338236844
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng rổ UEL',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8727969, 106.777692,
        'https://images.pexels.com/photos/8980690/pexels-photo-8980690.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.48, 105, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8693557/pexels-photo-8693557.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30619256/pexels-photo-30619256.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
        'Sân bóng rổ UEL', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 247000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        247000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 247000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá UEL (Hồ Chí Minh) — osm:way/1338236846
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá UEL',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8724568, 106.7775869,
        'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.59, 34, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/26694125/pexels-photo-26694125.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá UEL', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 441000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        441000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 441000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Cầu Trắng (Hồ Chí Minh) — osm:way/1368997054
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá Cầu Trắng',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        '21, Kênh Nước Đen, Bình Tân, Hồ Chí Minh',
        NULL,
        10.7883264, 106.6154665,
        'https://images.pexels.com/photos/186240/pexels-photo-186240.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.56, 14, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Cầu Trắng', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 576000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        576000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 576000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân cầu lông Tây Thạnh (Hồ Chí Minh) — osm:way/1369408164
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân cầu lông Tây Thạnh',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8101273, 106.6265994,
        'https://images.pexels.com/photos/26238656/pexels-photo-26238656.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.03, 94, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8007493/pexels-photo-8007493.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8007094/pexels-photo-8007094.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
        'Sân cầu lông Tây Thạnh', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 117000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        117000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 117000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá hẻm 278 Gò Xoài (Hồ Chí Minh) — osm:way/1369819610
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân bóng đá hẻm 278 Gò Xoài',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7896183, 106.6055786,
        'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.22, 56, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá hẻm 278 Gò Xoài', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 426000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        426000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 426000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng đá Mã Lò (Hồ Chí Minh) — osm:way/1369868812
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng đá Mã Lò',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7843274, 106.596581,
        'https://images.pexels.com/photos/15362139/pexels-photo-15362139.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.56, 74, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/13434565/pexels-photo-13434565.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng đá Mã Lò', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 391000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        391000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 391000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis Quyết Tâm (Hồ Chí Minh) — osm:way/1380229378
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Sân Tennis Quyết Tâm',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8461487, 106.6462674,
        'https://images.pexels.com/photos/30617588/pexels-photo-30617588.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.12, 66, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/36034890/pexels-photo-36034890.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/8224638/pexels-photo-8224638.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis Quyết Tâm', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 284000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        284000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 284000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân bóng Kiểm sát (Hồ Chí Minh) — osm:way/1393700000
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân bóng Kiểm sát',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8501, 106.7930896,
        'https://images.pexels.com/photos/4328745/pexels-photo-4328745.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 3.81, 89, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/8941562/pexels-photo-8941562.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/32090925/pexels-photo-32090925.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Sân bóng Kiểm sát', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 419000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        419000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 419000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Mansion Sports Club (Hồ Chí Minh) — osm:way/1411716048
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'Mansion Sports Club',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap. Website: https://www.facebook.com/mansionsportsclubvn',
        'Hồ Chí Minh',
        NULL,
        10.7161123, 106.7446185,
        'https://images.pexels.com/photos/46792/the-ball-stadion-football-the-pitch-46792.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.00, 96, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/30651230/pexels-photo-30651230.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'Mansion Sports Club', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap. Website: https://www.facebook.com/mansionsportsclubvn', 'FACILITY', complex_id,
        '06:00', '22:00', 371000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        371000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 371000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Sân Tennis 66 (Hồ Chí Minh) — osm:way/1417491431
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Sân Tennis 66',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8141432, 106.7277289,
        'https://images.pexels.com/photos/30894524/pexels-photo-30894524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.84, 20, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/27151849/pexels-photo-27151849.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6931243/pexels-photo-6931243.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Sân Tennis 66', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 187000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        187000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 187000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- San bóng ngoài trời (Hồ Chí Minh) — osm:way/1464064892
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
        'San bóng ngoài trời',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.8695042, 106.7911486,
        'https://images.pexels.com/photos/47343/the-ball-stadion-horn-corner-47343.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.74, 104, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/33210166/pexels-photo-33210166.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/14317118/pexels-photo-14317118.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
        'San bóng ngoài trời', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 347000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        347000.00 + (court_no * 10000),
        (ARRAY['FIVE_A_SIDE','SEVEN_A_SIDE','ELEVEN_A_SIDE'])[1 + floor(random() * 3)::int],
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 1) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 347000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;

-- Tennis Fans League (Hồ Chí Minh) — osm:way/1520908368
WITH new_complex AS (
    INSERT INTO stadium_complexes (
        owner_id, name, description, address, phone, latitude, longitude,
        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at
    ) VALUES (
        (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
        'Tennis Fans League',
        'Dữ liệu thực tế tổng hợp từ OpenStreetMap.',
        'Hồ Chí Minh',
        NULL,
        10.7987537, 106.7481629,
        'https://images.pexels.com/photos/4582494/pexels-photo-4582494.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'AVAILABLE', 'APPROVED', 4.44, 78, NOW()
    ) RETURNING complex_id
),
gallery_images AS (
    INSERT INTO stadium_complex_images (complex_id, image_url)
    SELECT complex_id, unnest(ARRAY['https://images.pexels.com/photos/3845084/pexels-photo-3845084.jpeg?auto=compress&cs=tinysrgb&h=650&w=940', 'https://images.pexels.com/photos/6010282/pexels-photo-6010282.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'])
    FROM new_complex
),
new_facility AS (
    INSERT INTO stadiums (
        sport_type_id, stadium_name, description, node_type, complex_id,
        open_time, close_time, price_per_hour, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
        'Tennis Fans League', 'Dữ liệu thực tế tổng hợp từ OpenStreetMap.', 'FACILITY', complex_id,
        '06:00', '22:00', 261000.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_complex
    RETURNING stadium_id, complex_id
),
new_courts AS (
    INSERT INTO stadiums (
        stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,
        price_per_hour, football_field_type, stadium_status, approved_status,
        average_rating, review_count, created_at
    )
    SELECT 'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '06:00', '22:00',
        261000.00 + (court_no * 10000),
        NULL,
        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()
    FROM new_facility f, generate_series(1, 2) AS court_no
    RETURNING stadium_id
),
link_sport_type AS (
    INSERT INTO complex_sport_types (complex_id, sport_type_id)
    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis')
    FROM new_complex
    ON CONFLICT DO NOTHING
)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 261000.00, 'AVAILABLE'
FROM new_courts, generate_series(6, 21) AS h;
