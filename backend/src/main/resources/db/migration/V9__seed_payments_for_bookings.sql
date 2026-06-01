-- ══════════════════════════════════════════════════════════════════════════
-- V9__seed_payments_for_bookings.sql
-- Seed dữ liệu mẫu cho bảng payments
-- Khắc phục việc thiếu bản ghi thanh toán cho các đơn đã PAID hoặc REFUNDED
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Đơn đặt sân số 1 (CONFIRMED - PAID)
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (1, 'VNPAY', 300000.00, 'TXN-CONFIRMED-001', 'SUCCESS', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 2. Đơn đặt sân số 3 (COMPLETED - PAID)
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (3, 'MOMO', 120000.00, 'TXN-COMPLETED-003', 'SUCCESS', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 3. Đơn đặt sân số 4 (CANCELLED - REFUNDED)
-- 3.1. Giao dịch thanh toán gốc
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (4, 'BANKING', 300000.00, 'TXN-CANCELLED-004', 'SUCCESS', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 3.2. Giao dịch hoàn tiền (Số tiền âm)
INSERT INTO payments (booking_id, payment_method, amount, transaction_code, payment_status, paid_at)
VALUES (4, 'BANKING', -300000.00, 'RFND-CANCELLED-004', 'SUCCESS', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
