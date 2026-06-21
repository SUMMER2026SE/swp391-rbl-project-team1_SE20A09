-- Thêm 1 đơn đặt sân trạng thái COMPLETED (Đã hoàn thành) nhưng CHƯA CÓ REVIEW
-- Để test chức năng viết đánh giá cho Sân Bóng Đá Thủ Đức
-- NOTE: reservation_date không được include ở đây vì cột này được thêm bởi V5.1
-- (chạy sau V4_7 trên fresh DB). V5.1 sẽ tự backfill từ booking_date.
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note)
SELECT
    u.user_id,
    s.stadium_id,
    t.slot_id,
    150000.00,
    'COMPLETED',
    'PAID',
    'Đơn hàng tạo ra để test chức năng Review'
FROM users u
JOIN stadiums s ON s.stadium_name = 'Sân Bóng Đá Thủ Đức'
JOIN time_slots t ON t.stadium_id = s.stadium_id AND t.slot_status = 'AVAILABLE'
WHERE u.email = 'customer@sportvenue.com'
  AND NOT EXISTS (
      SELECT 1 FROM bookings b WHERE b.note = 'Đơn hàng tạo ra để test chức năng Review'
  )
LIMIT 1;
