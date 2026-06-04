-- ══════════════════════════════════════════════════════════════════════════
-- V17__reinsert_good_stadium_and_delete_bad.sql
-- Xóa toàn bộ sân Mỹ Đình (bao gồm sân không có hình) và tạo lại 1 bản chuẩn.
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Xóa mọi thứ liên quan đến "Sân Bóng Đá Mỹ Đình" hiện có (để dọn dẹp sạch sẽ sân không có ảnh)
DELETE FROM payments WHERE booking_id IN (
    SELECT booking_id FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM reviews WHERE booking_id IN (
    SELECT booking_id FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM bookings WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM time_slots WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM stadium_images WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình';

-- 2. Tạo lại đúng 1 Sân Mỹ Đình CHUẨN (có hình, địa chỉ chuẩn)
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
    'Sân Bóng Đá Mỹ Đình', 'Sân chất lượng cao dành cho thi đấu chuyên nghiệp.', 'Nam Từ Liêm, Hà Nội', 350000.00, 22, '05:00', '23:00', 'AVAILABLE', 4.8;

-- 3. Thêm ảnh chuẩn
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1518605368461-1ee51a704da0?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình';

-- 4. Tạo Time Slot
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT stadium_id, CURRENT_DATE + make_interval(hours => 18), CURRENT_DATE + make_interval(hours => 20), 'BOOKED'
FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình';

-- 5. Đặt sân cho tất cả Customer (PENDING để test Hủy)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT u.user_id, s.stadium_id, t.slot_id, s.price_per_hour * 2, 'PENDING', 'UNPAID', 'Đơn chờ xác nhận, có thể test Hủy Sân'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
AND s.stadium_name = 'Sân Bóng Đá Mỹ Đình';
