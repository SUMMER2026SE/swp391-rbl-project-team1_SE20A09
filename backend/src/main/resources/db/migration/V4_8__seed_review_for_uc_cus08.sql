-- Seed 1 review đã có sẵn cho customer@sportvenue.com để test UC-CUS-08 (Sửa đánh giá)
-- Dựa trên booking được tạo bởi V4_7 (đơn COMPLETED chưa có review)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment)
SELECT
    b.booking_id,
    u.user_id,
    s.stadium_id,
    4,
    'Sân sạch sẽ, nhân viên nhiệt tình. Sẽ quay lại!'
FROM bookings b
JOIN users u ON u.email = 'customer@sportvenue.com'
JOIN stadiums s ON s.stadium_name = 'Sân Bóng Đá Thủ Đức'
WHERE b.user_id = u.user_id
  AND b.stadium_id = s.stadium_id
  AND b.note = 'Đơn hàng tạo ra để test chức năng Review'
  AND NOT EXISTS (
      SELECT 1 FROM reviews r WHERE r.booking_id = b.booking_id
  )
LIMIT 1;
