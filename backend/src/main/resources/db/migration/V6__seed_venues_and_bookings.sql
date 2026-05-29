-- ══════════════════════════════════════════════════════════════════════════
-- V6__seed_venues_and_bookings.sql
-- Dữ liệu mẫu để mọi thành viên test độc lập không cần chờ nhau
-- Mật khẩu tất cả tài khoản: password123
-- BCrypt hash: $2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Thêm Owner Users (để Huy test Owner flow) ──────────────────────────
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, user_rank, account_status, is_verified)
VALUES
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Xuân', 'Nguyễn Huy', '0900000003', 'owner2@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'Gold', 'Active', TRUE),
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Chí Anh', 'Lý Hào', '0912345679', 'customer2@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'Silver', 'Active', TRUE),
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Minh', 'Trần An', '0912345680', 'customer3@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'Bronze', 'Active', TRUE)
ON CONFLICT (email) DO NOTHING;

-- ── 2. Owner Profiles ──────────────────────────────────────────────────────
INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES
    ((SELECT user_id FROM users WHERE email = 'owner2@sportvenue.com'),
     'Huy Sport Center', 'TAX-987654321', '456 Sports Ave, District 3, HCMC', 'Approved')
ON CONFLICT (user_id) DO NOTHING;

-- ── 3. Stadiums — Đa dạng loại sân và trạng thái ──────────────────────────
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, price_per_hour, capacity, open_time, close_time, stadium_status, average_rating)
VALUES
    -- Sân bóng đá 1 (Available) — thuộc owner@sportvenue.com
    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Sân Bóng Đá Thủ Đức', 'Sân bóng đá mini 5vs5 chất lượng cao, cỏ nhân tạo thế hệ 3',
     '123 Võ Văn Ngân, Thủ Đức, TP.HCM', 300000.00, 10, '06:00', '22:00', 'Available', 4.5),

    -- Sân cầu lông (Available) — thuộc owner@sportvenue.com
    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
     'Sân Cầu Lông Quận 1', 'Hệ thống 6 sân cầu lông trong nhà, sàn gỗ chuyên nghiệp',
     '45 Lê Lợi, Quận 1, TP.HCM', 120000.00, 4, '07:00', '23:00', 'Available', 4.8),

    -- Sân bóng rổ (Available) — thuộc owner2@sportvenue.com
    ((SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
     'Sân Bóng Rổ Bình Thạnh', 'Sân bóng rổ ngoài trời có mái che, đèn LED chơi đêm',
     '78 Đinh Tiên Hoàng, Bình Thạnh, TP.HCM', 200000.00, 10, '06:00', '22:00', 'Available', 4.2),

    -- Sân tennis (Maintenance) — để test filter trạng thái
    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
     'Sân Tennis Phú Mỹ Hưng', 'Sân tennis tiêu chuẩn quốc tế, mặt sân clay',
     '10 Nguyễn Văn Linh, Quận 7, TP.HCM', 250000.00, 4, '06:00', '21:00', 'Maintenance', 4.6),

    -- Sân bóng đá lớn (Available) — thuộc owner2@sportvenue.com
    ((SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Sân Bóng Đá Gò Vấp', 'Sân 7vs7 mặt cỏ tự nhiên, phòng thay đồ riêng',
     '99 Quang Trung, Gò Vấp, TP.HCM', 500000.00, 14, '05:30', '22:30', 'Available', 4.3)
ON CONFLICT DO NOTHING;

-- ── 4. StadiumImages — Ảnh mẫu ────────────────────────────────────────────
INSERT INTO stadium_images (stadium_id, image_url)
SELECT s.stadium_id, 'https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800'
FROM stadiums s WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';

INSERT INTO stadium_images (stadium_id, image_url)
SELECT s.stadium_id, 'https://images.unsplash.com/photo-1567095761054-7a02e69e5c43?w=800'
FROM stadiums s WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

INSERT INTO stadium_images (stadium_id, image_url)
SELECT s.stadium_id, 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800'
FROM stadiums s WHERE s.stadium_name = 'Sân Bóng Rổ Bình Thạnh';

-- ── 5. TimeSlots — Khung giờ mẫu cho Sân Bóng Đá Thủ Đức ─────────────────
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT
    s.stadium_id,
    CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => h),
    CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => h + 1),
    CASE
        WHEN h IN (7, 9) THEN 'Booked'      -- Giả lập một số slot đã đặt
        ELSE 'Available'
    END
FROM stadiums s,
     generate_series(6, 21) AS h
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';

-- TimeSlots cho Sân Cầu Lông Quận 1
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
SELECT
    s.stadium_id,
    CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => h),
    CURRENT_DATE + INTERVAL '1 day' + make_interval(hours => h + 1),
    CASE WHEN h = 8 THEN 'Booked' ELSE 'Available' END
FROM stadiums s,
     generate_series(7, 22) AS h
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

-- ── 6. Bookings — Các trạng thái khác nhau để test ────────────────────────
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'Confirmed',
    'Paid',
    'Đặt sân cho trận giao hữu cuối tuần'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'Booked'
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức'
LIMIT 1;

INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'Pending',
    'Unpaid'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'Available'
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1'
LIMIT 1;

INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'Completed',
    'Paid'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'Available'
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1'
LIMIT 1 OFFSET 1;

INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'Cancelled',
    'Refunded'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'Available'
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức'
LIMIT 1 OFFSET 2;

-- ── 7. Reviews — Đánh giá sau khi hoàn thành ──────────────────────────────
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment)
SELECT
    b.booking_id,
    b.user_id,
    b.stadium_id,
    5,
    'Sân đẹp, nhân viên nhiệt tình. Sẽ quay lại lần sau!'
FROM bookings b
WHERE b.booking_status = 'Completed'
LIMIT 1
ON CONFLICT DO NOTHING;

-- ── 8. Accessories — Phụ kiện mẫu ────────────────────────────────────────
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Bóng đá số 5', 20000.00, 5, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Áo thi đấu (bộ 10 cái)', 50000.00, 3, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Cầu lông (hộp 12 quả)', 30000.00, 10, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Vợt cầu lông', 15000.00, 20, TRUE
FROM stadiums s WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

-- ── 9. Notifications — Thông báo mẫu cho Owner ────────────────────────────
INSERT INTO notifications (user_id, title, content, notification_type, is_read)
SELECT
    u.user_id,
    'Đơn đặt sân mới',
    'Bạn có một đơn đặt sân mới đang chờ xác nhận.',
    'Booking',
    FALSE
FROM users u WHERE u.email = 'owner@sportvenue.com';

INSERT INTO notifications (user_id, title, content, notification_type, is_read)
SELECT
    u.user_id,
    'Đánh giá mới từ khách hàng',
    'Khách hàng vừa để lại đánh giá 5 sao cho sân của bạn.',
    'Booking',
    FALSE
FROM users u WHERE u.email = 'owner@sportvenue.com';
