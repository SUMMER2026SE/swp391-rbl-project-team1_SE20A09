-- ══════════════════════════════════════════════════════════════════════════
-- V15__seed_diverse_bookings_and_cleanup.sql
-- Xóa các đơn đặt sân trùng lặp trước đó (Sân Pickleball)
-- và tạo mới 3 đơn đặt sân cho 3 SÂN KHÁC NHAU để test toàn bộ chức năng.
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Xóa các Booking cũ của Sân Pickleball để tránh trùng lặp hiển thị
DELETE FROM payments WHERE booking_id IN (
    SELECT b.booking_id FROM bookings b
    JOIN stadiums s ON b.stadium_id = s.stadium_id
    WHERE s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
);
DELETE FROM bookings WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
);

-- 2. Thêm 3 sân mới hoàn toàn (như trong giao diện mẫu của bạn)
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
    'Sân Bóng Đá Mỹ Đình', 'Sân chất lượng cao dành cho thi đấu chuyên nghiệp.', 'Nam Từ Liêm, Hà Nội', 350000.00, 22, '05:00', '23:00', 'AVAILABLE', 4.8
WHERE NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình');

INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
    'Sân bóng Thành Công', 'Sân cỏ nhân tạo mềm mại, chống trượt.', 'Quận 1, TP.HCM', 285000.00, 14, '06:00', '22:00', 'AVAILABLE', 4.5
WHERE NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Sân bóng Thành Công');

INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
    'Arena Sports Center', 'Trung tâm thể thao đa năng chuẩn quốc tế.', 'Quận 3, TP.HCM', 360000.00, 14, '06:00', '22:00', 'AVAILABLE', 4.7
WHERE NOT EXISTS (SELECT 1 FROM stadiums WHERE stadium_name = 'Arena Sports Center');

-- 3. Thêm ảnh cho 3 sân
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1518605368461-1ee51a704da0?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
AND NOT EXISTS (SELECT 1 FROM stadium_images si WHERE si.stadium_id = stadiums.stadium_id);

INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1705593813682-033ee2991df6?w=800' FROM stadiums WHERE stadium_name = 'Sân bóng Thành Công'
AND NOT EXISTS (SELECT 1 FROM stadium_images si WHERE si.stadium_id = stadiums.stadium_id);

INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1544698310-74ea9d1c8258?w=800' FROM stadiums WHERE stadium_name = 'Arena Sports Center'
AND NOT EXISTS (SELECT 1 FROM stadium_images si WHERE si.stadium_id = stadiums.stadium_id);

-- 4. Tạo time_slots (Mỗi sân có 1 khung giờ riêng biệt)
-- Sân Mỹ Đình: 18:00 - 20:00 (hôm nay)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT stadium_id, CURRENT_DATE + make_interval(hours => 18), CURRENT_DATE + make_interval(hours => 20), 'BOOKED'
FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
AND NOT EXISTS (SELECT 1 FROM time_slots ts WHERE ts.stadium_id = stadiums.stadium_id AND ts.start_time = CURRENT_DATE + make_interval(hours => 18));

-- Sân Thành Công: 18:00 - 20:00 (Hôm qua)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT stadium_id, CURRENT_DATE - INTERVAL '1 day' + make_interval(hours => 18), CURRENT_DATE - INTERVAL '1 day' + make_interval(hours => 20), 'BOOKED'
FROM stadiums WHERE stadium_name = 'Sân bóng Thành Công'
AND NOT EXISTS (SELECT 1 FROM time_slots ts WHERE ts.stadium_id = stadiums.stadium_id AND ts.start_time = CURRENT_DATE - INTERVAL '1 day' + make_interval(hours => 18));

-- Arena Sports Center: 16:00 - 18:00 (Ngày mai)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT stadium_id, CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => 16), CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => 18), 'BOOKED'
FROM stadiums WHERE stadium_name = 'Arena Sports Center'
AND NOT EXISTS (SELECT 1 FROM time_slots ts WHERE ts.stadium_id = stadiums.stadium_id AND ts.start_time = CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => 16));


-- 5. Tạo 3 Booking cho TẤT CẢ các Khách hàng (Đảm bảo mỗi sân 1 booking, không trùng lặp)

-- Booking 1: Sân Bóng Đá Mỹ Đình (PENDING) -> Dùng để test tính năng HỦY SÂN
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT u.user_id, s.stadium_id, t.slot_id, s.price_per_hour * 2, 'PENDING', 'UNPAID', 'Đơn chờ xác nhận, có thể test Hủy Sân'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
AND s.stadium_name = 'Sân Bóng Đá Mỹ Đình'
AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.user_id = u.user_id AND b.stadium_id = s.stadium_id AND b.slot_id = t.slot_id);

-- Booking 2: Sân bóng Thành Công (COMPLETED) -> Dùng để test tính năng VIẾT ĐÁNH GIÁ
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT u.user_id, s.stadium_id, t.slot_id, s.price_per_hour * 2, 'COMPLETED', 'PAID', 'Đơn đã hoàn thành, có thể test Viết Đánh Giá'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
AND s.stadium_name = 'Sân bóng Thành Công'
AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.user_id = u.user_id AND b.stadium_id = s.stadium_id AND b.slot_id = t.slot_id);

-- Booking 3: Arena Sports Center (CONFIRMED) -> Dùng để test tính năng LIÊN HỆ CHỦ SÂN
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT u.user_id, s.stadium_id, t.slot_id, s.price_per_hour * 2, 'CONFIRMED', 'PAID', 'Đơn đã xác nhận, có thể test Liên hệ chủ sân'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
AND s.stadium_name = 'Arena Sports Center'
AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.user_id = u.user_id AND b.stadium_id = s.stadium_id AND b.slot_id = t.slot_id);

-- 6. Thêm Payment cho Booking 2 và Booking 3
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
SELECT b.booking_id, 'VNPAY', b.total_price, 'VNP' || b.booking_id || 'COMPLETED', 'SUCCESS', CURRENT_TIMESTAMP
FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id
WHERE s.stadium_name = 'Sân bóng Thành Công'
AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.booking_id);

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
SELECT b.booking_id, 'MOMO', b.total_price, 'MOMO' || b.booking_id || 'CONFIRMED', 'SUCCESS', CURRENT_TIMESTAMP
FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id
WHERE s.stadium_name = 'Arena Sports Center'
AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.booking_id);
