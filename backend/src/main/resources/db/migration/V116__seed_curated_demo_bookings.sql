-- ══════════════════════════════════════════════════════════════════════════
-- V115__seed_curated_demo_bookings.sql — Bộ booking có chủ đích trên 11 venue
-- mới (V114), đủ trạng thái để demo Track 1/2/3 trong docs/demo_flow_assignment:
-- PENDING_PAYMENT (gần hết hạn giữ chỗ + còn hạn), PENDING (legacy chờ Owner
-- duyệt), CONFIRMED (VNPay/tiền mặt, gồm 1 booking trong 24h tới để demo
-- BookingReminderScheduler), AWAITING_CASH_PAYMENT (chờ Owner xác nhận thu
-- tiền), COMPLETED (trải dài ~2 tháng qua, đủ dữ liệu cho báo cáo doanh thu),
-- CANCELLED với đủ 3 mức hoàn tiền (100%/50%/0%) + 1 case OWNER_FAULT.
--
-- Mỗi booking có note bắt đầu bằng "[DEMO-xx]" duy nhất — dùng làm khoá tra
-- cứu ở V116 (complaint/report/refund-exception-request cần gắn vào booking
-- cụ thể) thay vì booking_id cứng, vì booking_id phụ thuộc thứ tự chèn.
--
-- expired_at/booking_date dùng NOW() +/- INTERVAL để mốc thời gian luôn hợp
-- lý bất kể ngày nào chạy migration này (không hardcode timestamp tuyệt đối).
-- ══════════════════════════════════════════════════════════════════════════

-- DEMO-01: Nhà thi đấu Quân khu 7 / Sân 1 (Basketball) — PENDING_PAYMENT/UNPAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, expired_at
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Quân khu 7' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Quân khu 7' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        195000.00, 10000.00, 'PENDING_PAYMENT', 'UNPAID',
        NOW() - INTERVAL '3 minutes', CURRENT_DATE + 0, '[DEMO-01] Vừa đặt sân, sắp hết hạn giữ chỗ (demo BookingExpiryScheduler).', NOW() + INTERVAL '90 seconds'
    ) RETURNING booking_id, total_price
)
SELECT booking_id FROM ins_booking;

-- DEMO-02: Nhà thi đấu Tương Mai / Sân 1 (Basketball) — PENDING_PAYMENT/UNPAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, expired_at
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '20:00:00'),
        185000.00, 10000.00, 'PENDING_PAYMENT', 'UNPAID',
        NOW(), CURRENT_DATE + 1, '[DEMO-02] Vừa đặt sân, còn nguyên 5 phút giữ chỗ chờ thanh toán.', NOW() + INTERVAL '5 minutes'
    ) RETURNING booking_id, total_price
)
SELECT booking_id FROM ins_booking;

-- DEMO-03: Cung Thể thao Tiên Sơn / Sân 1 (Badminton) — PENDING/UNPAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1') AND start_time = '18:00:00'),
        90000.00, 10000.00, 'PENDING', 'UNPAID',
        NOW() - INTERVAL '2 hours', CURRENT_DATE + 2, '[DEMO-03] Đặt sân theo luồng cũ (legacy), chờ Owner xác nhận.'
    ) RETURNING booking_id, total_price
)
SELECT booking_id FROM ins_booking;

-- DEMO-04: Nhà thi đấu đa năng Quận 7 / Sân 1 (Badminton) — PENDING/UNPAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        105000.00, 10000.00, 'PENDING', 'UNPAID',
        NOW() - INTERVAL '3 hours', CURRENT_DATE + 2, '[DEMO-04] Đặt sân theo luồng cũ (legacy), chờ Owner xác nhận hoặc từ chối.'
    ) RETURNING booking_id, total_price
)
SELECT booking_id FROM ins_booking;

-- DEMO-05: Sân vận động Chi Lăng / Sân 1 (Football) — CONFIRMED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1') AND start_time = '20:00:00'),
        304500.00, 14500.00, 'CONFIRMED', 'PAID',
        NOW() - INTERVAL '1 day', CURRENT_DATE + 5, '[DEMO-05] Đã thanh toán VNPay thành công, lịch chơi còn vài ngày nữa.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '1 day' + INTERVAL '5 minutes',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '1 day' + INTERVAL '5 minutes', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '1 day' + INTERVAL '5 minutes', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-06: Sân vận động Thống Nhất / Sân 1 (Football) — CONFIRMED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thống Nhất' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thống Nhất' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        346500.00, 16500.00, 'CONFIRMED', 'PAID',
        NOW() - INTERVAL '2 days', CURRENT_DATE + 7, '[DEMO-06] Đã thanh toán VNPay thành công, đặt sân bóng đá tuần sau.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '2 days' + INTERVAL '3 minutes',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '2 days' + INTERVAL '3 minutes', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '2 days' + INTERVAL '3 minutes', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-07: Sân bóng rổ Quân Khu 5 / Sân 1 (Basketball) — CONFIRMED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, reminder_sent_at
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '18:00:00'),
        190000.00, 10000.00, 'CONFIRMED', 'PAID',
        NOW() - INTERVAL '6 hours', CURRENT_DATE + 1, '[DEMO-07] Đã thanh toán, lịch chơi trong vòng 24h tới (demo BookingReminderScheduler).', NULL
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '6 hours' + INTERVAL '2 minutes',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '6 hours' + INTERVAL '2 minutes', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '6 hours' + INTERVAL '2 minutes', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-08: Sân vận động Thanh Khê / Sân 1 (Football) — CONFIRMED/AWAITING_CASH_PAYMENT
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thanh Khê' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thanh Khê' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1') AND start_time = '20:00:00'),
        283500.00, 13500.00, 'CONFIRMED', 'AWAITING_CASH_PAYMENT',
        NOW() - INTERVAL '4 hours', CURRENT_DATE + 2, '[DEMO-08] Khách chọn thanh toán tiền mặt, chờ Owner xác nhận đã thu tiền.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'CASH', total_price, 'PENDING',
    NULL, 'CASH-' || booking_id, NULL, NULL
FROM ins_booking;

-- DEMO-09: Sân vận động Hàng Đẫy / Sân 1 (Football) — CONFIRMED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Hàng Đẫy' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Hàng Đẫy' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        378000.00, 18000.00, 'CONFIRMED', 'PAID',
        NOW() - INTERVAL '1 day', CURRENT_DATE + 1, '[DEMO-09] Thanh toán tiền mặt, Owner đã xác nhận đã thu tiền.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'CASH', total_price, 'SUCCESS',
    NOW() - INTERVAL '2 hours',
    'CASH-' || booking_id,
    NULL,
    NULL
FROM ins_booking;

-- DEMO-10: Cung Thể thao Tiên Sơn / Sân 1 (Basketball) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '18:00:00'),
        180000.00, 10000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '6 days', CURRENT_DATE + -5, '[DEMO-10] Đã chơi xong, thanh toán VNPay.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '6 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '6 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '6 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-11: Cung thể thao Quần Ngựa / Sân 1 (Badminton) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung thể thao Quần Ngựa' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung thể thao Quần Ngựa' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        100000.00, 10000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '11 days', CURRENT_DATE + -10, '[DEMO-11] Đã chơi xong, thanh toán VNPay.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '11 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '11 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '11 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-12: Nhà thi đấu Quân khu 7 / Sân 1 (Basketball) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Quân khu 7' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Quân khu 7' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '18:00:00'),
        195000.00, 10000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '16 days', CURRENT_DATE + -15, '[DEMO-12] Đã chơi xong, thanh toán tiền mặt.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'CASH', total_price, 'SUCCESS',
    NOW() - INTERVAL '16 days',
    'CASH-' || booking_id,
    NULL,
    NULL
FROM ins_booking;

-- DEMO-13: Sân vận động Chi Lăng / Sân 2 (Football) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2') AND start_time = '20:00:00'),
        315000.00, 15000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '21 days', CURRENT_DATE + -20, '[DEMO-13] Đã chơi xong, thanh toán VNPay.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '21 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '21 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '21 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-14: Nhà thi đấu Tương Mai / Sân 1 (Basketball) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1') AND start_time = '19:00:00'),
        185000.00, 10000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '26 days', CURRENT_DATE + -25, '[DEMO-14] Đã chơi xong, thanh toán tiền mặt.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'CASH', total_price, 'SUCCESS',
    NOW() - INTERVAL '26 days',
    'CASH-' || booking_id,
    NULL,
    NULL
FROM ins_booking;

-- DEMO-15: Nhà thi đấu đa năng Quận 7 / Sân 1 (Badminton) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1') AND start_time = '18:00:00'),
        105000.00, 10000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '36 days', CURRENT_DATE + -35, '[DEMO-15] Đã chơi xong, thanh toán VNPay.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '36 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '36 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '36 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-16: Sân vận động Thanh Khê / Sân 2 (Football) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thanh Khê' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thanh Khê' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2') AND start_time = '20:00:00'),
        294000.00, 14000.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '46 days', CURRENT_DATE + -45, '[DEMO-16] Đã chơi xong, thanh toán VNPay.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '46 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '46 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '46 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-17: Sân vận động Hàng Đẫy / Sân 2 (Football) — COMPLETED/PAID
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Hàng Đẫy' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Hàng Đẫy' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2') AND start_time = '19:00:00'),
        388500.00, 18500.00, 'COMPLETED', 'PAID',
        NOW() - INTERVAL '56 days', CURRENT_DATE + -55, '[DEMO-17] Đã chơi xong, thanh toán tiền mặt.'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'CASH', total_price, 'SUCCESS',
    NOW() - INTERVAL '56 days',
    'CASH-' || booking_id,
    NULL,
    NULL
FROM ins_booking;

-- DEMO-18: Sân bóng rổ Quân Khu 5 / Sân 2 (Basketball) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2') AND start_time = '18:00:00'),
        200000.00, 10000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '3 days', CURRENT_DATE + 6, '[DEMO-18] Lý do hủy hoàn tiền: Khách đổi lịch, hủy sớm trước >24h — hoàn 100%.', 'Khách đổi lịch, hủy sớm trước >24h'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '3 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '3 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '3 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-19: Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng / Sân 2 (Tennis) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng' AND sp.sport_name = 'Tennis' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng' AND sp.sport_name = 'Tennis' AND s.stadium_name = 'Sân 2') AND start_time = '19:00:00'),
        252000.00, 12000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '5 days', CURRENT_DATE + 9, '[DEMO-19] Lý do hủy hoàn tiền: Khách bận việc đột xuất, hủy sớm trước >24h — hoàn 100%.', 'Khách bận việc đột xuất, hủy sớm trước >24h'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '5 days',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '5 days', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '5 days', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-20: Sân vận động Thống Nhất / Sân 2 (Football) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thống Nhất' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Thống Nhất' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2') AND start_time = '20:00:00'),
        357000.00, 17000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '12 hours', CURRENT_DATE + 3, '[DEMO-20] Lý do hủy hoàn tiền: Khách hủy trong khoảng 12-24h trước giờ chơi — hoàn 50%.', 'Khách hủy trong khoảng 12-24h trước giờ chơi'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '12 hours',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '12 hours', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '12 hours', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-21: Cung thể thao Quần Ngựa / Sân 2 (Badminton) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung thể thao Quần Ngựa' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Cung thể thao Quần Ngựa' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 2') AND start_time = '18:00:00'),
        110000.00, 10000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '18 hours', CURRENT_DATE + 4, '[DEMO-21] Lý do hủy hoàn tiền: Khách hủy trong khoảng 12-24h trước giờ chơi — hoàn 50%.', 'Khách hủy trong khoảng 12-24h trước giờ chơi'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '18 hours',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '18 hours', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '18 hours', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-22: Nhà thi đấu Tương Mai / Sân 2 (Basketball) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2') AND start_time = '19:00:00'),
        195000.00, 10000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '2 hours', CURRENT_DATE + 1, '[DEMO-22] Lý do hủy hoàn tiền: Khách hủy sát giờ (<12h trước giờ chơi) — hoàn 0%. Ứng viên gửi Refund Exception Request.', 'Khách hủy sát giờ (<12h trước giờ chơi)'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '2 hours',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '2 hours', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '2 hours', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-23: Nhà thi đấu đa năng Quận 7 / Sân 3 (Badminton) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 3'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 3') AND start_time = '20:00:00'),
        125000.00, 10000.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '1 hour', CURRENT_DATE + 2, '[DEMO-23] Lý do hủy hoàn tiền: Khách hủy sát giờ (<12h trước giờ chơi) — hoàn 0%.', 'Khách hủy sát giờ (<12h trước giờ chơi)'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '1 hour',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '1 hour', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '1 hour', 'YYYYMMDDHH24MISS')
FROM ins_booking;

-- DEMO-24: Sân vận động Chi Lăng / Sân 3 (Football) — CANCELLED/REFUNDED
WITH ins_booking AS (
    INSERT INTO bookings (
        user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status,
        booking_date, reservation_date, note, cancel_reason
    ) VALUES (
        (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
        (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 3'),
        (SELECT slot_id FROM time_slots WHERE stadium_id = (SELECT s.stadium_id FROM stadiums s
        JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
        JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
        JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
        WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 3') AND start_time = '18:00:00'),
        325500.00, 15500.00, 'CANCELLED', 'REFUNDED',
        NOW() - INTERVAL '1 day', CURRENT_DATE + 4, '[DEMO-24] Lý do hủy hoàn tiền: Owner báo sân bảo trì đột xuất (lỗi từ phía sân) — hoàn 100% bất kể thời điểm hủy.', 'Owner báo sân bảo trì đột xuất (lỗi từ phía sân)'
    ) RETURNING booking_id, total_price
)
INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code, gateway_transaction_no, gateway_pay_date)
SELECT booking_id, 'VNPAY', total_price, 'SUCCESS',
    NOW() - INTERVAL '1 day',
    'VNP' || booking_id || to_char(NOW() - INTERVAL '1 day', 'YYYYMMDDHH24MISS'),
    (10000000 + booking_id)::text,
    to_char(NOW() - INTERVAL '1 day', 'YYYYMMDDHH24MISS')
FROM ins_booking;

