-- ══════════════════════════════════════════════════════════════════════════
-- V14__seed_pickleball_for_all_customers.sql
-- Đảm bảo tất cả các tài khoản Customer (bao gồm cả tài khoản bạn vừa tạo) 
-- đều có dữ liệu mẫu đặt sân Pickleball để xem trên giao diện.
-- ══════════════════════════════════════════════════════════════════════════

-- Thêm Booking cho tất cả Customers chưa có đơn ở sân Pickleball này
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT
    u.user_id,
    s.stadium_id,
    t.slot_id,
    s.price_per_hour,
    'COMPLETED',
    'PAID',
    'Đặt sân chơi Pickleball giao hữu cuối tuần.'
FROM users u
CROSS JOIN stadiums s
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'BOOKED'
WHERE u.role_id = (SELECT role_id FROM roles WHERE role_name = 'Customer')
  AND s.stadium_name = 'Sân Pickleball Chuẩn Quốc Tế'
  AND NOT EXISTS (
      SELECT 1 FROM bookings b2 
      WHERE b2.user_id = u.user_id 
        AND b2.slot_id = t.slot_id
  );

-- Thêm Payment cho các Booking vừa tạo
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
