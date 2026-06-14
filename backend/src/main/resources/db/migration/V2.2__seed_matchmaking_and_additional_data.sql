-- ══════════════════════════════════════════════════════════════════════════
-- V2.2__seed_matchmaking_and_additional_data.sql — Additional Test Seed Data
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Seed Additional Users & Owners (Phục vụ Lock/Unlock của Hào & Duyệt Owner của Hoàng) ──
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status, is_verified)
VALUES
    -- Chủ sân ở trạng thái chờ duyệt (PENDING)
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Khánh', 'Lê Minh', '0900000004', 'pending_owner@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'BRONZE', 'ACTIVE', TRUE),
    
    -- Khách hàng ở trạng thái bị khóa (BLOCKED) để test Unlock
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Nam', 'Trần Hoài', '0912345681', 'blocked_customer@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'BRONZE', 'BLOCKED', TRUE),

    -- Khách hàng chưa xác thực email (is_verified = FALSE)
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Sơn', 'Phan Văn', '0912345682', 'unverified_customer@sportvenue.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', NULL, 0, 'BRONZE', 'PENDING', FALSE);

-- Profile chủ sân chờ duyệt
INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES
    ((SELECT user_id FROM users WHERE email = 'pending_owner@sportvenue.com'), 
     'Green Field Sports Center', 'TAX-555666777', '789 Phan Văn Trị, Gò Vấp, TP.HCM', 'PENDING');

-- ── 2. Seed Additional Stadiums & TimeSlots (Phục vụ Duyệt Sân & Đặt Sân) ──
-- Sân bóng chuyền chờ duyệt (PENDING)
INSERT INTO stadiums (owner_id, sport_type_id, stadium_name, description, address, latitude, longitude, open_time, close_time, price_per_hour, capacity, stadium_status, approved_status, average_rating)
VALUES
    ((SELECT owner_id FROM owners WHERE business_name = 'Huy Sport Center'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Volleyball'),
     'Sân Bóng Chuyền Gò Vấp (Chờ Duyệt)', 'Sân bóng chuyền ngoài trời tiêu chuẩn.',
     '99 Quang Trung, Gò Vấp, TP.HCM', 10.835, 106.678, '06:00', '22:00', 120000.00, 12, 'AVAILABLE', 'PENDING', 5.0);

-- Tạo khung giờ (TimeSlots) cho các sân còn lại để đảm bảo tất cả sân đều có thể đặt được
-- Sân Bóng Rổ Bình Thạnh (Khung giờ từ 15h đến 20h)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT
    s.stadium_id,
    make_time(h, 0, 0),
    make_time(h + 1, 0, 0),
    100000.00,
    'AVAILABLE'
FROM stadiums s, generate_series(15, 20) AS h
WHERE s.stadium_name = 'Sân Bóng Rổ Bình Thạnh';

-- Sân Bóng Đá Gò Vấp (Khung giờ tối từ 17h đến 21h)
INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)
SELECT
    s.stadium_id,
    make_time(h, 0, 0),
    make_time(h + 1, 0, 0),
    160000.00,
    'AVAILABLE'
FROM stadiums s, generate_series(17, 21) AS h
WHERE s.stadium_name = 'Sân Bóng Đá Gò Vấp';

-- ── 3. Seed Additional Bookings & Payments (Phục vụ Dashboard của Lượng & Lịch sử của An) ──
-- Booking 4: Trận đấu bóng rổ hôm qua đã hoàn thành và trả bằng ví MOMO
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, booking_date)
VALUES
    ((SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh'),
     (SELECT slot_id FROM time_slots ts JOIN stadiums s ON ts.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Rổ Bình Thạnh' LIMIT 1),
     100000.00, 'COMPLETED', 'PAID', CURRENT_DATE - INTERVAL '1 day');

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES
    ((SELECT COALESCE(MAX(booking_id), 0) FROM bookings), 'MOMO', 100000.00, 'TXN-DASHBOARD-004', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Booking 5: Trận đấu bóng đá Gò Vấp hôm nay đã thanh toán VNPAY (CONFIRMED)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, booking_date)
VALUES
    ((SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp'),
     (SELECT slot_id FROM time_slots ts JOIN stadiums s ON ts.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Gò Vấp' LIMIT 1),
     160000.00, 'CONFIRMED', 'PAID', CURRENT_DATE);

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES
    ((SELECT COALESCE(MAX(booking_id), 0) FROM bookings), 'VNPAY', 160000.00, 'TXN-DASHBOARD-005', 'SUCCESS', CURRENT_TIMESTAMP);

-- Booking 6: Đơn đặt sân thanh toán Thất Bại (PENDING & UNPAID)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, booking_date)
VALUES
    ((SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1'),
     (SELECT slot_id FROM time_slots ts JOIN stadiums s ON ts.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Cầu Lông Quận 1' LIMIT 1 OFFSET 2),
     60000.00, 'PENDING', 'UNPAID', CURRENT_DATE);

INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES
    ((SELECT COALESCE(MAX(booking_id), 0) FROM bookings), 'VNPAY', 60000.00, 'TXN-FAILED-006', 'FAILED', CURRENT_TIMESTAMP);

-- ── 4. Seed MatchRequests (Ghép kèo - USP) ──────────────────────────────────
INSERT INTO match_requests (user_id, stadium_id, sport_type_id, title, description, play_date, start_time, end_time, max_players, current_players, skill_level, split_price, price_per_player, match_status)
VALUES
    -- Kèo 1: Cần người đá bóng cỏ nhân tạo (Đang mở - OPEN)
    ((SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Tìm 3 đồng đội đá bóng 5vs5 tối mai',
     'Nhóm mình đang có 7 người, cần tìm thêm 3 bạn đá giao lưu vui vẻ. Trình độ trung bình, không đá xấu.',
     CURRENT_DATE + INTERVAL '1 day', '18:00:00', '19:30:00', 10, 8, 'INTERMEDIATE', TRUE, 45000.00, 'OPEN'),

    -- Kèo 2: Ghép sân cầu lông (Đã đầy - FULL)
    ((SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Cầu Lông Quận 1'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Badminton'),
     'Giao lưu cầu lông đôi nam nữ',
     'Cần ghép thêm 1 cặp hoặc 2 bạn lẻ vào đánh đôi. Sân trong nhà thoáng mát.',
     CURRENT_DATE + INTERVAL '2 days', '08:00:00', '10:00:00', 4, 4, 'ADVANCED', FALSE, 0.00, 'FULL'),

    -- Kèo 3: Ghép bóng rổ Bình Thạnh (Đã hủy - CANCELLED)
    ((SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Basketball'),
     'Cần người ném rổ chung chiều nay',
     'Tìm các bạn đam mê bóng rổ qua làm vài hiệp 3x3.',
     CURRENT_DATE, '17:00:00', '19:00:00', 6, 2, 'BEGINNER', FALSE, 0.00, 'CANCELLED'),

    -- Kèo 4: Trận đấu đã hoàn thành trong quá khứ (COMPLETED)
    ((SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp'),
     (SELECT sport_type_id FROM sport_types WHERE sport_name = 'Football'),
     'Đá giao hữu sân Gò Vấp tuần trước',
     'Trận đấu giao lưu chia tiền sân.',
     CURRENT_DATE - INTERVAL '5 days', '19:00:00', '21:30:00', 14, 14, 'INTERMEDIATE', TRUE, 50000.00, 'COMPLETED');

-- ── 5. Seed JoinRequests ───────────────────────────────────────────────────
INSERT INTO join_requests (match_id, user_id, request_status, message)
VALUES
    -- Yêu cầu gửi tới Kèo 1 (Bóng đá Thủ Đức)
    ((SELECT match_id FROM match_requests WHERE title = 'Tìm 3 đồng đội đá bóng 5vs5 tối mai'),
     (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
     'PENDING', 'Cho mình đăng ký 1 slot nhé bạn, đá vị trí tiền đạo hoặc cánh.'),

    ((SELECT match_id FROM match_requests WHERE title = 'Tìm 3 đồng đội đá bóng 5vs5 tối mai'),
     (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
     'APPROVED', 'Đã liên hệ host và được xác nhận tham gia.'),

    -- Yêu cầu gửi tới Kèo 2 (Cầu lông Q1)
    ((SELECT match_id FROM match_requests WHERE title = 'Giao lưu cầu lông đôi nam nữ'),
     (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     'APPROVED', 'Mình đăng ký 2 slot nam nữ nhé.'),

    -- Yêu cầu gửi tới Kèo 3 (Bóng rổ)
    ((SELECT match_id FROM match_requests WHERE title = 'Cần người ném rổ chung chiều nay'),
     (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     'REJECTED', 'Bận việc đột xuất không tham gia được.'),

    ((SELECT match_id FROM match_requests WHERE title = 'Cần người ném rổ chung chiều nay'),
     (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
     'CANCELLED', 'Mình xin rút slot nhé.');

-- ── 6. Seed Complaints (Phục vụ module Operation của Hoàng) ───────────────
INSERT INTO complaints (booking_id, user_id, subject, content, status, response)
VALUES
    -- Khiếu nại 1: Đang mở (OPEN)
    (1, 
     (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     'Sân quá trơn trượt',
     'Trận đấu tối qua tại sân bóng đá Thủ Đức rất nguy hiểm do mặt cỏ nhân tạo bị ướt và đọng nước nhưng không được làm sạch trước giờ thuê. Đề nghị chủ sân kiểm tra và cải thiện chất lượng sân.',
     'OPEN', NULL),

    -- Khiếu nại 2: Đã giải quyết (RESOLVED)
    (3, 
     (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
     'Trùng giờ đặt sân',
     'Tôi đặt sân cầu lông Quận 1 từ 8h-9h nhưng khi đến sân thì có một nhóm khác đang chơi và họ báo cũng đã đặt slot này. Đề nghị hệ thống kiểm tra lại.',
     'RESOLVED', 'Admin phản hồi: Hệ thống đã kiểm tra lỗi kỹ thuật và hoàn lại 100% tiền sân cho quý khách. ');

-- ── 7. Seed Additional Reviews (Phục vụ module Moderation của Hào) ──────────
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response)
VALUES
    -- Đánh giá 1: Chưa có phản hồi từ Owner
    (4, 
     (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Rổ Bình Thạnh'),
     4, 'Sân gỗ đẹp, ánh sáng tốt. Tuy nhiên bãi đỗ xe hơi chật vào giờ cao điểm.', NULL),

    -- Đánh giá 2: Đã có phản hồi của Owner
    (5, 
     (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
     (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Gò Vấp'),
     5, 'Cỏ nhân tạo êm, đá rất sướng chân. Sẽ quay lại thường xuyên.', 
     'Cảm ơn quý khách đã ủng hộ sân! Chúng tôi sẽ tiếp tục duy trì dịch vụ tốt nhất.');
