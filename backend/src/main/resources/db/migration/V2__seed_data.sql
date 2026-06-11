-- ══════════════════════════════════════════════════════════════════════════
-- V2__seed_data.sql — Consolidated Seed Data
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Roles & Sport Types ────────────────────────────────────────────────
INSERT INTO roles (role_name) VALUES ('Admin'), ('Owner'), ('Customer');

INSERT INTO sport_types (sport_name) VALUES
    ('Football'), ('Badminton'), ('Basketball'), ('Tennis'), ('Volleyball');

-- ── 2. Users ──────────────────────────────────────────────────────────────
-- BCrypt hash of 'password123'
-- is_verified = TRUE, account_status = 'ACTIVE'
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status, is_verified)
VALUES
    ((SELECT role_id FROM roles WHERE role_name = 'Admin'), 'Admin', 'System', '0900000001', 'admin@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=admin', 100, 'GOLD', 'ACTIVE', TRUE),
    
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Hoàng', 'Mai Huy', '0900000002', 'owner@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 50, 'SILVER', 'ACTIVE', TRUE),
    
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Xuân', 'Nguyễn Huy', '0900000003', 'owner2@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'GOLD', 'ACTIVE', TRUE),
    
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Hoàng', 'Mai Huy', '0912345678', 'customer@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=customer', 0, 'BRONZE', 'ACTIVE', TRUE),
    
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Chí Anh', 'Lý Hào', '0912345679', 'customer2@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'SILVER', 'ACTIVE', TRUE),
    
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Minh', 'Trần An', '0912345680', 'customer3@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'BRONZE', 'ACTIVE', TRUE);

-- ── 3. Owners ─────────────────────────────────────────────────────────────
INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES
    ((SELECT user_id FROM users WHERE email = 'owner@sportvenue.com'), 'Sport Venue Owner Corp', 'TAX-123456789', '123 Sports Road, District 1, HCMC', 'APPROVED'),
    ((SELECT user_id FROM users WHERE email = 'owner2@sportvenue.com'), 'Huy Sport Center', 'TAX-987654321', '456 Sports Ave, District 3, HCMC', 'APPROVED');

-- ── 4. Stadiums ───────────────────────────────────────────────────────────
-- latitude DOUBLE PRECISION, longitude DOUBLE PRECISION
-- NO capacity, NO price_per_hour
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, latitude, longitude, open_time, close_time, price_per_hour, capacity, stadium_status, approved_status, average_rating)
VALUES
    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Sân Bóng Đá Thủ Đức', 'Sân bóng đá mini 5vs5 chất lượng cao, cỏ nhân tạo thế hệ 3',
     '123 Võ Văn Ngân, Thủ Đức, TP.HCM', 10.850, 106.755, '06:00', '22:00', 150000.00, 10, 'AVAILABLE', 'APPROVED', 4.5),

    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
     'Sân Cầu Lông Quận 1', 'Hệ thống 6 sân cầu lông trong nhà, sàn gỗ chuyên nghiệp',
     '45 Lê Lợi, Quận 1, TP.HCM', 10.776, 106.700, '07:00', '23:00', 150000.00, 10, 'AVAILABLE', 'APPROVED', 4.8),

    ((SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
     'Sân Bóng Rổ Bình Thạnh', 'Sân bóng rổ ngoài trời có mái che, đèn LED chơi đêm',
     '78 Đinh Tiên Hoàng, Bình Thạnh, TP.HCM', 10.800, 106.711, '06:00', '22:00', 150000.00, 10, 'AVAILABLE', 'APPROVED', 4.2),

    ((SELECT owner_id FROM owners WHERE business_name = 'Sport Venue Owner Corp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Tennis'),
     'Sân Tennis Phú Mỹ Hưng', 'Sân tennis tiêu chuẩn quốc tế, mặt sân clay',
     '10 Nguyễn Văn Linh, Quận 7, TP.HCM', 10.732, 106.722, '06:00', '21:00', 150000.00, 10, 'MAINTENANCE', 'APPROVED', 4.6),

    ((SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Sân Bóng Đá Gò Vấp', 'Sân 7vs7 mặt cỏ tự nhiên, phòng thay đồ riêng',
     '99 Quang Trung, Gò Vấp, TP.HCM', 10.835, 106.678, '05:30', '22:30', 150000.00, 10, 'AVAILABLE', 'APPROVED', 4.3);

-- ── 5. Stadium Images ─────────────────────────────────────────────────────
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức';
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1567095761054-7a02e69e5c43?w=800' FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1';
INSERT INTO stadium_images (stadium_id, image_url)
SELECT stadium_id, 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800' FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh';

-- ── 6. Time Slots ─────────────────────────────────────────────────────────
-- start_time TIME, end_time TIME, price_per_slot DECIMAL(10,2)
-- Sân bóng đá Thủ Đức
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT
    s.stadium_id,
    make_time(h, 0, 0),
    make_time(h + 1, 0, 0),
    150000.00,
    CASE WHEN h IN (7, 9) THEN 'BOOKED' ELSE 'AVAILABLE' END
FROM stadiums s, generate_series(6, 21) AS h
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';

-- Sân Cầu lông Q1
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT
    s.stadium_id,
    make_time(h, 0, 0),
    make_time(h + 1, 0, 0),
    60000.00,
    CASE WHEN h = 8 THEN 'BOOKED' ELSE 'AVAILABLE' END
FROM stadiums s, generate_series(7, 22) AS h
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

-- ── 7. Amenities ──────────────────────────────────────────────────────────
INSERT INTO amenities (name, icon) VALUES
    ('Wifi', 'wifi'),
    ('Bãi đỗ xe', 'car'),
    ('Căng tin', 'coffee'),
    ('Thuê giày', 'shopping-bag'),
    ('Phòng thay đồ', 'user'),
    ('Nước uống miễn phí', 'droplet');

-- Map Amenities
INSERT INTO stadium_amenities (stadium_id, amenity_id)
SELECT s.stadium_id, a.amenity_id 
FROM stadiums s, amenities a 
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức' AND a.name IN ('Wifi', 'Bãi đỗ xe', 'Căng tin', 'Phòng thay đồ', 'Nước uống miễn phí');

INSERT INTO stadium_amenities (stadium_id, amenity_id)
SELECT s.stadium_id, a.amenity_id 
FROM stadiums s, amenities a 
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1' AND a.name IN ('Wifi', 'Bãi đỗ xe', 'Thuê giày', 'Nước uống miễn phí');

-- ── 8. Bookings ───────────────────────────────────────────────────────────
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    t.price_per_slot,
    'CONFIRMED',
    'PAID',
    'Đặt sân cho trận giao hữu cuối tuần'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức'
LIMIT 1;

INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    t.price_per_slot,
    'PENDING',
    'UNPAID'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'AVAILABLE'
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1'
LIMIT 1;

INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    s.stadium_id,
    t.slot_id,
    t.price_per_slot,
    'COMPLETED',
    'PAID'
FROM stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'AVAILABLE'
WHERE s.stadium_name = 'Sân Cầu Lông Quận 1'
LIMIT 1 OFFSET 1;

-- ── 9. Payments ───────────────────────────────────────────────────────────
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES 
    (1, 'VNPAY', 150000.00, 'TXN-CONFIRMED-001', 'SUCCESS', CURRENT_TIMESTAMP),
    (3, 'MOMO', 60000.00, 'TXN-COMPLETED-003', 'SUCCESS', CURRENT_TIMESTAMP);

-- ── 10. Reviews ───────────────────────────────────────────────────────────
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment)
SELECT
    b.booking_id,
    b.user_id,
    b.stadium_id,
    5,
    'Sân đẹp, nhân viên nhiệt tình. Sẽ quay lại lần sau!'
FROM bookings b
WHERE b.booking_status = 'COMPLETED'
LIMIT 1;

-- ── 11. Accessories ───────────────────────────────────────────────────────
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Bóng đá số 5', 20000.00, 5, TRUE FROM stadiums s WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức';
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT s.stadium_id, 'Vợt cầu lông', 15000.00, 20, TRUE FROM stadiums s WHERE s.stadium_name = 'Sân Cầu Lông Quận 1';

-- ── 12. Notifications ─────────────────────────────────────────────────────
INSERT INTO notifications (user_id, notification_type, title, message, is_read)
SELECT user_id, 'BOOKING', 'Đơn đặt sân mới', 'Bạn có một đơn đặt sân mới đang chờ xác nhận.', FALSE
FROM users WHERE email = 'owner@sportvenue.com';

-- ── 13. User Favorite Stadiums ──────────────────────────────────────────────
INSERT INTO user_favorite_stadiums (user_id, stadium_id)
SELECT u.user_id, s.stadium_id
FROM users u
CROSS JOIN stadiums s
WHERE u.email = 'customer@sportvenue.com'
  AND s.stadium_name IN ('Sân Bóng Đá Thủ Đức', 'Sân Cầu Lông Quận 1')
ON CONFLICT DO NOTHING;
