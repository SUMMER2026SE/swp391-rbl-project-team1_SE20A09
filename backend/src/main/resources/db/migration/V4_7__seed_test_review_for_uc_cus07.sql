-- Thêm 1 đơn đặt sân trạng thái COMPLETED (Đã hoàn thành) nhưng CHƯA CÓ REVIEW
-- Để test chức năng viết đánh giá cho Sân Bóng Đá Thủ Đức (stadiumId = 1)
INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, note, reservation_date)
SELECT
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    (SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Thủ Đức'),
    (SELECT slot_id FROM time_slots t JOIN stadiums s ON t.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Thủ Đức' AND t.slot_status = 'AVAILABLE' LIMIT 1),
    150000.00,
    'COMPLETED',
    'PAID',
    'Đơn hàng tạo ra để test chức năng Review',
    CURRENT_DATE - INTERVAL '7 days'
WHERE NOT EXISTS (
    SELECT 1 FROM bookings b
    JOIN stadiums s ON b.stadium_id = s.stadium_id
    WHERE b.booking_status = 'COMPLETED'
    AND s.stadium_name = 'Sân Bóng Đá Thủ Đức'
    AND b.user_id = (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com')
);
