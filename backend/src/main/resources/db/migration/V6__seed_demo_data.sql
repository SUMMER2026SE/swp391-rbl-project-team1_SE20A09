-- ══════════════════════════════════════════════════════════════════════════
-- V6__seed_demo_data.sql — Comprehensive Demo Seed Data
-- Mục tiêu: Đủ dữ liệu đa dạng để demo toàn bộ UC mà không cần tạo thủ công
-- ══════════════════════════════════════════════════════════════════════════

-- ── 0. Kích hoạt lại sân Tennis (đổi MAINTENANCE → AVAILABLE cho demo) ─────
UPDATE stadiums SET stadium_status = 'AVAILABLE' WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';

-- ── 1. Thêm Users phục vụ đa dạng demo ─────────────────────────────────────
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status, is_verified)
VALUES
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Tuấn', 'Phạm Gia',    '0912345685', 'customer4@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=c4', 130, 'GOLD',   'ACTIVE', TRUE),

    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Linh', 'Nguyễn Thị',  '0912345686', 'customer5@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=c5',  45, 'SILVER', 'ACTIVE', TRUE),

    -- Owner mới: PENDING — dùng để demo UC-ADM-06 (Approve Owner)
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Hùng', 'Trương Văn', '0900000006', 'owner3@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'BRONZE', 'ACTIVE', TRUE);

INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES ((SELECT user_id FROM users WHERE email = 'owner3@sportvenue.com'),
        'Hùng Sport Arena', 'TAX-444555666', '200 Lý Thường Kiệt, Quận 10, TP.HCM', 'PENDING');

-- ── 2. Time Slots cho Sân Tennis (chưa có trong V2) ─────────────────────────
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT s.stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), 200000.00, 'AVAILABLE'
FROM stadiums s, generate_series(6, 20) AS h
WHERE s.stadium_name = 'Sân Tennis Phú Mỹ Hưng';

-- ── 3. Thêm ảnh cho tất cả các sân ──────────────────────────────────────────
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1459865264687-595d652de67e?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';

INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1554284126-aa88f22d8b74?w=800' FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1';
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1628891890377-57a0c4b55d41?w=800' FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1';

INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1583502756264-5b1aebfd0b3b?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';

INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800' FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1474546652694-a33dd8161d66?w=800' FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';

INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp';
INSERT INTO stadium_images (stadium_id, image_url) SELECT stadium_id, 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp';

-- ── 4. Tiện nghi cho các sân còn thiếu ──────────────────────────────────────
INSERT INTO stadium_amenities (stadium_id, amenity_id)
SELECT s.stadium_id, a.amenity_id FROM stadiums s, amenities a
WHERE s.stadium_name = 'Sân Bóng Rổ Bình Thạnh'
  AND a.name IN ('Wifi', 'Bãi đỗ xe', 'Nước uống miễn phí', 'Phòng thay đồ');

INSERT INTO stadium_amenities (stadium_id, amenity_id)
SELECT s.stadium_id, a.amenity_id FROM stadiums s, amenities a
WHERE s.stadium_name = 'Sân Tennis Phú Mỹ Hưng'
  AND a.name IN ('Wifi', 'Bãi đỗ xe', 'Thuê giày', 'Nước uống miễn phí', 'Phòng thay đồ');

INSERT INTO stadium_amenities (stadium_id, amenity_id)
SELECT s.stadium_id, a.amenity_id FROM stadiums s, amenities a
WHERE s.stadium_name = 'Sân Bóng Đá Gò Vấp'
  AND a.name IN ('Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ', 'Nước uống miễn phí');

-- ── 5. Phụ kiện cho thuê (accessories) ──────────────────────────────────────
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, acc.name, acc.price, acc.qty, TRUE
FROM stadiums s
CROSS JOIN (VALUES
    ('Bóng rổ', 25000.00, 4),
    ('Áo thi đấu', 15000.00, 20)
) AS acc(name, price, qty)
WHERE s.stadium_name = 'Sân Bóng Rổ Bình Thạnh';

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, acc.name, acc.price, acc.qty, TRUE
FROM stadiums s
CROSS JOIN (VALUES
    ('Vợt tennis', 30000.00, 10),
    ('Bóng tennis (3 quả)', 20000.00, 30)
) AS acc(name, price, qty)
WHERE s.stadium_name = 'Sân Tennis Phú Mỹ Hưng';

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, acc.name, acc.price, acc.qty, TRUE
FROM stadiums s
CROSS JOIN (VALUES
    ('Bóng đá số 5', 20000.00, 8),
    ('Áo thi đấu', 15000.00, 28)
) AS acc(name, price, qty)
WHERE s.stadium_name = 'Sân Bóng Đá Gò Vấp';

-- ── 6. Lịch sử booking 6 tháng — phục vụ biểu đồ doanh thu Dashboard ────────
DO $$
DECLARE
    v_c1 INTEGER; v_c2 INTEGER; v_c3 INTEGER; v_c4 INTEGER; v_c5 INTEGER;
    v_st_ftd INTEGER; v_st_bq1 INTEGER; v_st_bbt INTEGER; v_st_tpmh INTEGER; v_st_fgv INTEGER;
    v_sl_ftd INTEGER; v_sl_bq1 INTEGER; v_sl_bbt INTEGER; v_sl_tpmh INTEGER; v_sl_fgv INTEGER;
    v_bid INTEGER;
    v_date DATE;
    v_methods TEXT[] := ARRAY['VNPAY','MOMO','BANKING','CASH','VNPAY'];
BEGIN
    SELECT user_id INTO v_c1 FROM users WHERE email = 'customer@sportvenue.com';
    SELECT user_id INTO v_c2 FROM users WHERE email = 'customer2@sportvenue.com';
    SELECT user_id INTO v_c3 FROM users WHERE email = 'customer3@sportvenue.com';
    SELECT user_id INTO v_c4 FROM users WHERE email = 'customer4@sportvenue.com';
    SELECT user_id INTO v_c5 FROM users WHERE email = 'customer5@sportvenue.com';

    SELECT stadium_id INTO v_st_ftd  FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';
    SELECT stadium_id INTO v_st_bq1  FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1';
    SELECT stadium_id INTO v_st_bbt  FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';
    SELECT stadium_id INTO v_st_tpmh FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng';
    SELECT stadium_id INTO v_st_fgv  FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp';

    SELECT slot_id INTO v_sl_ftd  FROM time_slots WHERE stadium_id = v_st_ftd  AND start_time = '07:00:00' LIMIT 1;
    SELECT slot_id INTO v_sl_bq1  FROM time_slots WHERE stadium_id = v_st_bq1  AND start_time = '09:00:00' LIMIT 1;
    SELECT slot_id INTO v_sl_bbt  FROM time_slots WHERE stadium_id = v_st_bbt  AND start_time = '16:00:00' LIMIT 1;
    SELECT slot_id INTO v_sl_tpmh FROM time_slots WHERE stadium_id = v_st_tpmh AND start_time = '08:00:00' LIMIT 1;
    SELECT slot_id INTO v_sl_fgv  FROM time_slots WHERE stadium_id = v_st_fgv  AND start_time = '18:00:00' LIMIT 1;

    FOR i IN 1..72 LOOP
        v_date := CURRENT_DATE - (i * 2 + 1 || ' days')::INTERVAL;

        CASE (i % 5)
            WHEN 0 THEN
                INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
                VALUES (v_c1, v_st_ftd, v_sl_ftd, 150000.00, 'COMPLETED', 'PAID', v_date)
                RETURNING booking_id INTO v_bid;
                INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
                VALUES (v_bid, 'VNPAY', 150000.00, 'TXN-H' || i, 'SUCCESS', v_date::TIMESTAMP + INTERVAL '8 hours');

            WHEN 1 THEN
                INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
                VALUES (v_c2, v_st_bq1, v_sl_bq1, 60000.00, 'COMPLETED', 'PAID', v_date)
                RETURNING booking_id INTO v_bid;
                INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
                VALUES (v_bid, 'MOMO', 60000.00, 'TXN-H' || i, 'SUCCESS', v_date::TIMESTAMP + INTERVAL '9 hours');

            WHEN 2 THEN
                INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
                VALUES (v_c3, v_st_bbt, v_sl_bbt, 100000.00, 'COMPLETED', 'PAID', v_date)
                RETURNING booking_id INTO v_bid;
                INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
                VALUES (v_bid, 'BANKING', 100000.00, 'TXN-H' || i, 'SUCCESS', v_date::TIMESTAMP + INTERVAL '16 hours');

            WHEN 3 THEN
                INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
                VALUES (v_c4, v_st_tpmh, v_sl_tpmh, 200000.00, 'COMPLETED', 'PAID', v_date)
                RETURNING booking_id INTO v_bid;
                INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
                VALUES (v_bid, 'VNPAY', 200000.00, 'TXN-H' || i, 'SUCCESS', v_date::TIMESTAMP + INTERVAL '8 hours');

            ELSE
                INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
                VALUES (v_c5, v_st_fgv, v_sl_fgv, 160000.00, 'COMPLETED', 'PAID', v_date)
                RETURNING booking_id INTO v_bid;
                INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
                VALUES (v_bid, 'CASH', 160000.00, 'TXN-H' || i, 'SUCCESS', v_date::TIMESTAMP + INTERVAL '18 hours');
        END CASE;
    END LOOP;
END $$;

-- ── 7. Booking sắp tới (hôm nay & tương lai) — dùng cho luồng đặt sân demo ──

-- Booking CONFIRMED ngày mai - customer (đã trả VNPAY)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date, note)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức') AND start_time = '17:00:00' LIMIT 1),
    150000.00, 'CONFIRMED', 'PAID', CURRENT_DATE + INTERVAL '1 day', 'Trận giao hữu cùng đồng nghiệp';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'VNPAY', 150000.00, 'TXN-UPCOMING-001', 'SUCCESS', CURRENT_TIMESTAMP);

-- Booking CONFIRMED tuần tới - customer2 (Tennis)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng') AND start_time = '10:00:00' LIMIT 1),
    200000.00, 'CONFIRMED', 'PAID', CURRENT_DATE + INTERVAL '7 days';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'MOMO', 200000.00, 'TXN-UPCOMING-002', 'SUCCESS', CURRENT_TIMESTAMP);

-- Booking CONFIRMED 3 ngày tới - customer4 (Cầu lông)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1') AND start_time = '07:00:00' LIMIT 1),
    60000.00, 'CONFIRMED', 'PAID', CURRENT_DATE + INTERVAL '3 days';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'BANKING', 60000.00, 'TXN-UPCOMING-003', 'SUCCESS', CURRENT_TIMESTAMP);

-- Booking PENDING chờ xác nhận - customer5 (Bóng rổ) — dùng để demo Owner Confirm/Reject
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh') AND start_time = '17:00:00' LIMIT 1),
    100000.00, 'PENDING', 'UNPAID', CURRENT_DATE + INTERVAL '2 days';

-- ── 8. Booking đã hoàn thành gần đây — dùng để demo Review & Complaint ──────

-- COMPLETED 3 ngày trước - customer (Football TD) — dùng để demo Write Review
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức') AND start_time = '19:00:00' LIMIT 1),
    150000.00, 'COMPLETED', 'PAID', CURRENT_DATE - INTERVAL '3 days';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'VNPAY', 150000.00, 'TXN-REVIEW-001', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '3 days');

-- COMPLETED 5 ngày trước - customer (Tennis) — dùng để demo Submit Complaint
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng') AND start_time = '09:00:00' LIMIT 1),
    200000.00, 'COMPLETED', 'PAID', CURRENT_DATE - INTERVAL '5 days';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'MOMO', 200000.00, 'TXN-COMPLAINT-001', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '5 days');

-- Lưu booking_id này để tạo complaint
DO $$
DECLARE v_bid INTEGER;
BEGIN
    SELECT booking_id INTO v_bid FROM bookings
    WHERE reservation_date = CURRENT_DATE - INTERVAL '5 days'
      AND stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Tennis Phú Mỹ Hưng')
      AND user_id = (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com')
    LIMIT 1;

    INSERT INTO complaints (booking_id, user_id, subject, content, status, priority)
    VALUES (v_bid,
            (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
            'Sân không đúng mô tả trên website',
            'Tôi đặt sân tennis với mô tả mặt sân clay chuẩn quốc tế nhưng đến nơi thấy mặt sân bị trầy và ướt. Đèn sân cũng không đủ sáng dù đã đặt khung giờ 9h sáng.',
            'OPEN', 'MEDIUM');
END $$;

-- COMPLETED 7 ngày trước - customer2 (Football GV) — Complaint đang IN_PROGRESS
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp'),
    (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp') AND start_time = '19:00:00' LIMIT 1),
    160000.00, 'COMPLETED', 'PAID', CURRENT_DATE - INTERVAL '7 days';

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (currval('bookings_booking_id_seq'), 'VNPAY', 160000.00, 'TXN-COMPLAINT-002', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '7 days');

DO $$
DECLARE v_bid INTEGER;
BEGIN
    SELECT booking_id INTO v_bid FROM bookings
    WHERE reservation_date = CURRENT_DATE - INTERVAL '7 days'
      AND stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp')
      AND user_id = (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com')
    LIMIT 1;

    INSERT INTO complaints (booking_id, user_id, subject, content, status, response, priority)
    VALUES (v_bid,
            (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
            'Phòng thay đồ bị khóa khi đến sân',
            'Nhóm tôi 7 người đặt sân từ 19h nhưng phòng thay đồ bị khóa và nhân viên sân không có mặt trong suốt 30 phút đầu. Ảnh hưởng nghiêm trọng đến thời gian chơi.',
            'IN_PROGRESS', 'Chủ sân đã xem xét và liên hệ xin lỗi. Đang xử lý đền bù.', 'HIGH');
END $$;

-- ── 9. Reviews đa dạng cho nhiều sân ────────────────────────────────────────
-- DISTINCT ON (user_id, stadium_id) đảm bảo không vi phạm unique_user_stadium_review
WITH eligible AS (
    SELECT DISTINCT ON (b.user_id, b.stadium_id)
        b.booking_id, b.user_id, b.stadium_id
    FROM bookings b
    WHERE b.booking_status = 'COMPLETED'
      AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.booking_id = b.booking_id)
      AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.user_id = b.user_id AND r.stadium_id = b.stadium_id)
    ORDER BY b.user_id, b.stadium_id, b.booking_id
),
numbered AS (
    SELECT booking_id, user_id, stadium_id,
           ROW_NUMBER() OVER (ORDER BY booking_id) AS rn
    FROM eligible
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment)
SELECT
    booking_id, user_id, stadium_id,
    CASE (rn % 6) WHEN 0 THEN 5 WHEN 1 THEN 4 WHEN 2 THEN 5 WHEN 3 THEN 3 WHEN 4 THEN 4 ELSE 5 END,
    CASE (rn % 6)
        WHEN 0 THEN 'Sân cực đẹp, cỏ nhân tạo thế hệ mới, không bị nóng dù chơi lúc trưa. Sẽ quay lại!'
        WHEN 1 THEN 'Sân sạch sẽ, giá hợp lý. Nhân viên hỗ trợ nhanh khi có sự cố nhỏ.'
        WHEN 2 THEN 'Tiện ích đầy đủ, wifi mạnh, bãi đỗ xe rộng rãi. Vị trí thuận tiện.'
        WHEN 3 THEN 'Sân ổn nhưng đèn giờ tối hơi tối. Nên cải thiện thêm ánh sáng.'
        WHEN 4 THEN 'Dịch vụ tốt, đặt sân online nhanh chóng. Chủ sân phản hồi ngay khi cần.'
        ELSE 'Chất lượng sân rất ổn, mặt sân bằng phẳng. Phù hợp cả người mới và pro.'
    END
FROM numbered
LIMIT 15;

-- Thêm owner response qua subquery (PostgreSQL không hỗ trợ UPDATE...LIMIT trực tiếp)
UPDATE reviews SET owner_response = 'Cảm ơn bạn đã tin tưởng! Chúng tôi sẽ cải thiện hệ thống đèn sân trong tháng tới.'
WHERE review_id = (
    SELECT review_id FROM reviews
    WHERE stadium_id = (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức')
      AND rating_score = 3 AND owner_response IS NULL
    LIMIT 1
);

UPDATE reviews SET owner_response = 'Cảm ơn bạn đã góp ý! Chúng tôi rất vui được phục vụ. Hẹn gặp lại lần sau!'
WHERE review_id IN (
    SELECT review_id FROM reviews
    WHERE stadium_id IN (SELECT stadium_id FROM stadiums WHERE stadium_name IN ('Sân Bóng Đá Gò Vấp', 'Sân Tennis Phú Mỹ Hưng'))
      AND rating_score >= 4 AND owner_response IS NULL
    LIMIT 2
);

-- ── 10. Kèo ghép (Match Requests) bổ sung cho demo ─────────────────────────
INSERT INTO match_requests (user_id, stadium_id, sport_type_id, title, description, play_date, start_time, end_time,
                            max_players, current_players, skill_level, split_price, price_per_player, match_status, matching_type)
VALUES
    -- Kèo OPEN ngày mai — Football 5vs5 — dùng để demo Join Request
    ((SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     '[DEMO] Cần 3 người đá bóng 5vs5 tối mai — vui vẻ là chính',
     'Nhóm 7 người cần thêm 3 bạn. Mức độ trung bình, chơi giao lưu không tranh thắng thua. Chia tiền sân đều.',
     CURRENT_DATE + INTERVAL '1 day', '19:00:00', '20:30:00',
     10, 7, 'INTERMEDIATE', TRUE, 40000.00, 'OPEN', 'INDIVIDUAL'),

    -- Kèo OPEN 3 ngày tới — Badminton đôi
    ((SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
     '[DEMO] Tìm đối thủ đánh cầu lông đôi — Trình độ khá',
     'Hai vợ chồng mình muốn tìm thêm 1 đôi để đánh giao hữu cuối tuần. Yêu cầu trình độ khá trở lên.',
     CURRENT_DATE + INTERVAL '3 days', '08:00:00', '10:00:00',
     4, 2, 'ADVANCED', FALSE, 0.00, 'OPEN', 'TEAM_VS_TEAM'),

    -- Kèo OPEN tuần tới — Basketball 3vs3
    ((SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
     '[DEMO] Ghép 3vs3 bóng rổ — tìm thêm 2 chiến hữu',
     'Nhóm 4 người muốn đá 3vs3. Cần 2 bạn nữa. Trình độ beginner cũng được, chơi vui là chính.',
     CURRENT_DATE + INTERVAL '5 days', '17:00:00', '19:00:00',
     6, 4, 'BEGINNER', TRUE, 30000.00, 'OPEN', 'INDIVIDUAL');

-- Join request vào Kèo DEMO Football — customer2 đang PENDING (để Owner duyệt)
INSERT INTO join_requests (match_id, user_id, request_status, message)
SELECT
    (SELECT mr.match_id FROM match_requests mr
     JOIN sport_types st ON mr.sport_type_id = st.sport_type_id
     WHERE mr.title LIKE '[DEMO]%' AND st.sport_name = 'Football' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    'PENDING',
    'Cho mình đăng ký 1 slot nhé! Mình đá vị trí tiền vệ, trình độ trung bình, chơi nghiêm túc.';

-- Join request đã được APPROVED — customer3 vào kèo Football
INSERT INTO join_requests (match_id, user_id, request_status, message)
SELECT
    (SELECT mr.match_id FROM match_requests mr
     JOIN sport_types st ON mr.sport_type_id = st.sport_type_id
     WHERE mr.title LIKE '[DEMO]%' AND st.sport_name = 'Football' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
    'APPROVED',
    'Mình có thể chơi thủ môn hoặc hậu vệ. Đã xác nhận với host.';

-- ── 11. Thông báo mới cho Owner và Customer ──────────────────────────────────
INSERT INTO notifications (user_id, notification_type, title, message, is_read)
SELECT user_id, 'BOOKING', 'Đơn đặt sân mới cần xác nhận',
       'Khách hàng mới vừa đặt sân Bóng Rổ Bình Thạnh vào ' || (CURRENT_DATE + INTERVAL '2 days')::TEXT || '. Vui lòng xác nhận.', FALSE
FROM users WHERE email = 'owner2@sportvenue.com';

INSERT INTO notifications (user_id, notification_type, title, message, is_read)
SELECT user_id, 'BOOKING', 'Đặt sân thành công',
       'Đơn đặt sân Bóng Đá Thủ Đức ngày ' || (CURRENT_DATE + INTERVAL '1 day')::TEXT || ' đã được xác nhận!', FALSE
FROM users WHERE email = 'customer@sportvenue.com';

INSERT INTO notifications (user_id, notification_type, title, message, is_read)
SELECT user_id, 'SYSTEM', 'Chào mừng đến với SportVenue!',
       'Cảm ơn bạn đã đăng ký. Khám phá hàng trăm sân thể thao gần bạn ngay hôm nay.', TRUE
FROM users WHERE email IN ('customer4@sportvenue.com', 'customer5@sportvenue.com');

INSERT INTO notifications (user_id, notification_type, title, message, is_read)
SELECT user_id, 'BOOKING', 'Có người muốn tham gia kèo của bạn',
       'customer2@sportvenue.com vừa gửi yêu cầu tham gia kèo ghép bóng đá của bạn.', FALSE
FROM users WHERE email = 'customer4@sportvenue.com';
