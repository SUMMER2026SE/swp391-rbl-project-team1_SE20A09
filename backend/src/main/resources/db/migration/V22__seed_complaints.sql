-- Seed dữ liệu mẫu cho bảng complaints (khiếu nại)
-- Gắn với các Booking đã được seed ở V6 hoặc V11

-- Khiếu nại 1: OPEN (Mới)
INSERT INTO complaints (booking_id, user_id, content, status, response, created_at)
VALUES (
    (SELECT booking_id FROM bookings ORDER BY booking_id ASC LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'),
    'Sân bóng cỏ nhân tạo chất lượng không tốt, cỏ bị rách nhiều chỗ gây trơn trượt khi chạy.',
    'OPEN',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '2 days'
);

-- Khiếu nại 2: IN_PROGRESS (Đang xử lý)
INSERT INTO complaints (booking_id, user_id, content, status, response, created_at)
VALUES (
    (SELECT booking_id FROM bookings ORDER BY booking_id ASC LIMIT 1 OFFSET 1),
    (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'),
    'Đèn chiếu sáng sân bóng buổi tối quá mờ, không đủ ánh sáng để thi đấu.',
    'IN_PROGRESS',
    '[{"from":"Chủ sân","message":"Chào bạn, chúng tôi đã tiếp nhận phản hồi và đang tiến hành thay thế bóng đèn LED mới.","time":"2026-06-01 10:00"}]',
    CURRENT_TIMESTAMP - INTERVAL '1 days'
);

-- Khiếu nại 3: RESOLVED (Đã giải quyết)
INSERT INTO complaints (booking_id, user_id, content, status, response, created_at)
VALUES (
    (SELECT booking_id FROM bookings ORDER BY booking_id ASC LIMIT 1 OFFSET 2),
    (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'),
    'Tôi đã đặt sân nhưng khi đến thì bị trùng lịch với đoàn khác, rất thất vọng về khâu quản lý.',
    'RESOLVED',
    '[{"from":"Chủ sân","message":"Đã giải quyết: Chào bạn, chân thành xin lỗi bạn về sự cố trùng lịch. Chúng tôi đã tiến hành hoàn tiền 100% kèm mã giảm giá 20% cho lần đặt sau.","time":"2026-06-02 08:30"}]',
    CURRENT_TIMESTAMP - INTERVAL '3 days'
);
