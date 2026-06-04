-- ══════════════════════════════════════════════════════════════════════════
-- V13__seed_pickleball_data.sql
-- Thêm dữ liệu sân Pickleball và lịch sử đặt sân mẫu cho khách hàng
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Thêm môn thể thao Pickleball nếu chưa có
INSERT INTO sport_types (sport_name)
VALUES ('Pickleball')
ON CONFLICT (sport_name) DO NOTHING;

-- 2. Thêm sân Pickleball cho Owner 1 (Sport Venue Owner Corp)
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Pickleball'),
    'Sân Pickleball Chuẩn Quốc Tế',
    'Sân Pickleball ngoài trời cao cấp, mặt sân sơn Acrylic nhiều lớp chống trượt, đèn LED chuyên nghiệp.',
    'Khu đô thị Sala, Quận 2, TP.HCM',
    150000.00,
    4,
    '05:00',
    '23:00',
    'AVAILABLE',
    4.9
WHERE NOT EXISTS (
    SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
);

-- 3. Thêm ảnh cho sân Pickleball
INSERT INTO stadium_images (stadium_id, image_url)
SELECT s.stadium_id, 'https://images.unsplash.com/photo-1622359487532-613d526fcbe6?w=800'
FROM stadiums s
WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
AND NOT EXISTS (
    SELECT 1 FROM stadium_images si WHERE si.stadium_id = s.stadium_id
);

-- 4. Thêm TimeSlots (khung giờ mẫu)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT
    s.stadium_id,
    CURRENT_DATE + make_interval(hours => h),
    CURRENT_DATE + make_interval(hours => h + 1),
    CASE
        WHEN h IN (17, 18) THEN 'BOOKED'
        ELSE 'AVAILABLE'
    END
FROM stadiums s,
     generate_series(15, 20) AS h
WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
AND NOT EXISTS (
    SELECT 1 FROM time_slots ts WHERE ts.stadium_id = s.stadium_id AND ts.start_time = CURRENT_DATE + make_interval(hours => h)
);

-- 5. Thêm Booking cho Customer (customer@sportvenue.com)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'COMPLETED',
    'PAID',
    'Đặt sân chơi Pickleball giao hữu cuối tuần.'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
LIMIT 1;

-- 6. Thêm Payment cho Booking vừa tạo
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
SELECT
    b.booking_id,
    'VNPAY',
    b.total_price,
    'VNP' || b.booking_id || 'PICKLE',
    'SUCCESS',
    CURRENT_TIMESTAMP
FROM bookings b
JOIN stadiums s ON b.stadium_id = s.stadium_id
WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
AND b.note = 'Đặt sân chơi Pickleball giao hữu cuối tuần.'
AND NOT EXISTS (
    SELECT 1 FROM payments p WHERE p.booking_id = b.booking_id
);

-- 7. Thêm Phụ kiện (Accessories)
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Vợt Pickleball sợi Carbon', 30000.00, 10, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
AND NOT EXISTS (SELECT 1 FROM accessories a WHERE a.stadium_id = s.stadium_id AND a.name = 'Vợt Pickleball sợi Carbon');

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Bóng Pickleball (Hộp 3 quả)', 15000.00, 50, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
AND NOT EXISTS (SELECT 1 FROM accessories a WHERE a.stadium_id = s.stadium_id AND a.name = 'Bóng Pickleball (Hộp 3 quả)');
