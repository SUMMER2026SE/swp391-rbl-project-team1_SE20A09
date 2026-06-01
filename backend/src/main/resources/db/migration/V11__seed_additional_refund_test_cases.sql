-- ══════════════════════════════════════════════════════════════════════════
-- V10__seed_additional_refund_test_cases.sql
-- Seed dữ liệu mẫu bổ sung cho kịch bản kiểm thử Hoàn tiền (Refund Scenarios)
-- Các giờ chơi được tạo ĐỘNG theo thời gian hiện tại để không bao giờ bị cũ
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Thêm 3 TimeSlots động cho Sân Bóng Đá Thủ Đức (stadium_id = 1) ──
-- Slot A: 48 tiếng nữa (Thỏa mãn luồng hoàn tiền 100%)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
VALUES (1, 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '2 days'), 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '1 hour'), 
        'BOOKED')
ON CONFLICT DO NOTHING;

-- Slot B: 18 tiếng nữa (Thỏa mãn luồng hoàn tiền 50%)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
VALUES (1, 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '18 hours'), 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '18 hours' + INTERVAL '1 hour'), 
        'BOOKED')
ON CONFLICT DO NOTHING;

-- Slot C: 6 tiếng nữa (Thỏa mãn luồng hoàn tiền 0%)
INSERT INTO time_slots (stadium_id, start_time, end_time, slot_status)
VALUES (1, 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '6 hours'), 
        date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '6 hours' + INTERVAL '1 hour'), 
        'BOOKED')
ON CONFLICT DO NOTHING;


-- ── 2. Tạo Bookings tương ứng cho 3 Slot vừa tạo ──
-- Booking A (Hoàn 100%): Giá trị booking_id tự sinh
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
VALUES (
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    1,
    (SELECT slot_id FROM time_slots WHERE stadium_id = 1 AND start_time = date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '2 days') LIMIT 1),
    300000.00,
    'CONFIRMED',
    'PAID',
    'Test hoàn tiền 100% (Huỷ trước >= 24h)'
);

-- Booking B (Hoàn 50%)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
VALUES (
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    1,
    (SELECT slot_id FROM time_slots WHERE stadium_id = 1 AND start_time = date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '18 hours') LIMIT 1),
    300000.00,
    'CONFIRMED',
    'PAID',
    'Test hoàn tiền 50% (Huỷ trước 12h - 24h)'
);

-- Booking C (Hoàn 0%)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
VALUES (
    (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
    1,
    (SELECT slot_id FROM time_slots WHERE stadium_id = 1 AND start_time = date_trunc('hour', CURRENT_TIMESTAMP + INTERVAL '6 hours') LIMIT 1),
    300000.00,
    'CONFIRMED',
    'PAID',
    'Test hoàn tiền 0% (Huỷ trước < 12h)'
);


-- ── 3. Tạo các giao dịch gốc tương ứng trong bảng payments ──
-- Giao dịch cho Booking A
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (
    (SELECT booking_id FROM bookings WHERE note = 'Test hoàn tiền 100% (Huỷ trước >= 24h)' LIMIT 1),
    'VNPAY',
    300000.00,
    'TXN-TEST-REFUND-100PCT',
    'SUCCESS',
    CURRENT_TIMESTAMP
);

-- Giao dịch cho Booking B
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (
    (SELECT booking_id FROM bookings WHERE note = 'Test hoàn tiền 50% (Huỷ trước 12h - 24h)' LIMIT 1),
    'MOMO',
    300000.00,
    'TXN-TEST-REFUND-50PCT',
    'SUCCESS',
    CURRENT_TIMESTAMP
);

-- Giao dịch cho Booking C
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (
    (SELECT booking_id FROM bookings WHERE note = 'Test hoàn tiền 0% (Huỷ trước < 12h)' LIMIT 1),
    'BANKING',
    300000.00,
    'TXN-TEST-REFUND-0PCT',
    'SUCCESS',
    CURRENT_TIMESTAMP
);
