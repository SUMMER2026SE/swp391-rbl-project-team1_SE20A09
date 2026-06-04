-- ══════════════════════════════════════════════════════════════════════════
-- V19__seed_new_booking_k34.sql
-- Mô phỏng hành động User vừa đặt thành công một sân mới.
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Thêm một sân bóng mới toanh
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
SELECT
    (SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
    (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
    'Sân Bóng Đá Cỏ Nhân Tạo K34', 'Sân mới nâng cấp, mặt cỏ mượt, ánh sáng siêu sáng chuẩn FIFA.', 'Bạch Đằng, Tân Bình, TP.HCM', 250000.00, 14, '05:00', '23:00', 'AVAILABLE', 4.9;

-- 2. Thêm hình ảnh đẹp cho sân
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Cỏ Nhân Tạo K34';

-- 3. Đăng ký khung giờ 18:00 - 20:00 cho ngày hôm nay (trạng thái đã bị đặt)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT stadium_id, CURRENT_DATE + make_interval(hours => 18), CURRENT_DATE + make_interval(hours => 20), 'BOOKED'
FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Cỏ Nhân Tạo K34';

-- 4. Tiến hành "Đặt Sân" cho Customer (Trạng thái: PENDING - Chờ xác nhận)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT u.user_id, s.stadium_id, t.slot_id, s.price_per_hour * 2, 'PENDING', 'UNPAID', 'Đơn đặt sân mới nhất, đang chờ xác nhận từ chủ sân'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
AND s.stadium_name = 'Sân Bóng Đá Cỏ Nhân Tạo K34';
