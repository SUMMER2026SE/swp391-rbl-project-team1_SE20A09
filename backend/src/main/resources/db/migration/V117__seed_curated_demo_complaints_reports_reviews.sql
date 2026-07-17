-- ══════════════════════════════════════════════════════════════════════════
-- V116__seed_curated_demo_complaints_reports_reviews.sql — Refund Exception
-- Request, Complaint (đủ 6 trạng thái), Report (đủ 3 chiều: Customer→Owner,
-- Owner→Customer, Host↔Joiner cộng đồng), Review — gắn vào các booking đã
-- tạo ở V115 (tra theo note '[DEMO-xx]' vì booking_id phụ thuộc thứ tự chèn).
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Refund Exception Request — 2 case, gắn vào 2 booking hoàn 0% ở V115 ──
-- RER-1: mới nộp, chờ Owner duyệt (Track 2: Owner review refund-exception)
INSERT INTO refund_exception_requests (booking_id, customer_id, reason, status, created_at, expires_at)
SELECT b.booking_id, b.user_id,
    'Em bị ốm đột xuất phải nhập viện nên không kịp báo hủy sớm hơn, mong chủ sân xem xét hoàn lại một phần.',
    'PENDING_OWNER', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour' + INTERVAL '72 hours'
FROM bookings b WHERE b.note LIKE '[DEMO-22]%';

-- RER-2: Owner đã từ chối, đã escalate lên Admin (Track 5: admin final decision)
INSERT INTO refund_exception_requests (booking_id, customer_id, reason, status, owner_note, created_at, owner_reviewed_at, expires_at)
SELECT b.booking_id, b.user_id,
    'Xe hỏng giữa đường nên phải hủy sát giờ, em có ảnh chụp hiện trường đính kèm.',
    'PENDING_ADMIN',
    'Lý do chưa đủ thuyết phục theo chính sách hoàn tiền của sân, khách đã khiếu nại lên Admin.',
    NOW() - INTERVAL '20 hours', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '20 hours' + INTERVAL '72 hours'
FROM bookings b WHERE b.note LIKE '[DEMO-23]%';

-- ── 2. Complaints — đủ 6 trạng thái, mỗi cái gắn 1 booking COMPLETED khác nhau ──
-- OPEN
INSERT INTO complaints (booking_id, user_id, subject, content, status, created_at)
SELECT b.booking_id, b.user_id, 'Sân đọng nước giữa sân',
    'Sân có vệt nước đọng ở giữa sân hôm em chơi, khá trơn trượt và ảnh hưởng thi đấu.',
    'OPEN', NOW() - INTERVAL '5 days'
FROM bookings b WHERE b.note LIKE '[DEMO-10]%';

-- IN_PROGRESS
INSERT INTO complaints (booking_id, user_id, subject, content, status, response, created_at)
SELECT b.booking_id, b.user_id, 'Máy lạnh trong phòng thay đồ không hoạt động',
    'Phòng thay đồ hôm đó không có điều hòa, rất nóng và bí.',
    'IN_PROGRESS',
    '[{"from":"Chủ sân","message":"Cảm ơn phản ánh của bạn, chúng tôi đang cho kiểm tra và sửa chữa hệ thống điều hòa.","time":"' || to_char(NOW() - INTERVAL '9 days', 'YYYY-MM-DD HH24:MI') || '"}]',
    NOW() - INTERVAL '10 days'
FROM bookings b WHERE b.note LIKE '[DEMO-11]%';

-- AWAITING_CUSTOMER_RESPONSE (Owner đã đề xuất giải pháp, chờ khách phản hồi)
INSERT INTO complaints (booking_id, user_id, subject, content, status, response, customer_response_deadline, created_at)
SELECT b.booking_id, b.user_id, 'Thiếu phụ kiện đã đặt kèm',
    'Em có thuê thêm bóng rổ dự phòng nhưng đến nơi nhân viên báo hết hàng, không được hoàn phí phụ kiện.',
    'AWAITING_CUSTOMER_RESPONSE',
    '[{"from":"Chủ sân","message":"Đã đề xuất giải pháp: Hoàn lại 100% phí phụ kiện đã thanh toán vào ví của bạn.","time":"' || to_char(NOW() - INTERVAL '3 days', 'YYYY-MM-DD HH24:MI') || '"}]',
    NOW() + INTERVAL '2 days', NOW() - INTERVAL '4 days'
FROM bookings b WHERE b.note LIKE '[DEMO-12]%';

-- ESCALATED (đã chuyển lên Admin xử lý)
INSERT INTO complaints (booking_id, user_id, subject, content, status, response, escalated_at, escalation_reason, created_at)
SELECT b.booking_id, b.user_id, 'Sân bị đặt trùng giờ với người khác',
    'Em đến sân thì thấy nhóm khác cũng đang chơi giờ em đã đặt và thanh toán, rất bất tiện.',
    'ESCALATED',
    '[{"from":"Hệ thống","message":"Khiếu nại đã được chuyển lên Ban quản trị. Lý do: Chủ sân chưa phản hồi quá thời hạn quy định.","time":"' || to_char(NOW() - INTERVAL '2 days', 'YYYY-MM-DD HH24:MI') || '"}]',
    NOW() - INTERVAL '2 days', 'Chủ sân chưa phản hồi quá thời hạn quy định.', NOW() - INTERVAL '6 days'
FROM bookings b WHERE b.note LIKE '[DEMO-13]%';

-- RESOLVED (Admin đã xử lý xong)
INSERT INTO complaints (booking_id, user_id, subject, content, status, response, resolved_at, admin_reviewed_by, admin_reviewed_at, created_at)
SELECT b.booking_id, b.user_id, 'Nhân viên sân thái độ không tốt',
    'Nhân viên hướng dẫn nhận sân khá thiếu nhiệt tình, mong cải thiện.',
    'RESOLVED',
    '[{"from":"Chủ sân","message":"Xin lỗi vì trải nghiệm không tốt, chúng tôi đã nhắc nhở nhân viên liên quan.","time":"' || to_char(NOW() - INTERVAL '7 days', 'YYYY-MM-DD HH24:MI') || '"},{"from":"Ban quản trị","message":"Khiếu nại đã được xử lý thỏa đáng, đóng khiếu nại.","time":"' || to_char(NOW() - INTERVAL '6 days', 'YYYY-MM-DD HH24:MI') || '"}]',
    NOW() - INTERVAL '6 days', (SELECT user_id FROM users WHERE email = 'admin@sportvenue.com'), NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '8 days'
FROM bookings b WHERE b.note LIKE '[DEMO-14]%';

-- CUSTOMER_WITHDRAWN (khách tự rút khiếu nại)
INSERT INTO complaints (booking_id, user_id, subject, content, status, response, created_at)
SELECT b.booking_id, b.user_id, 'Giá sân hiển thị sai',
    'Giá hiển thị lúc đặt khác với giá em bị trừ, mong kiểm tra lại.',
    'CUSTOMER_WITHDRAWN',
    '[{"from":"Khách hàng","message":"Em đã kiểm tra lại và thấy mình nhầm khung giờ, xin rút lại khiếu nại, xin lỗi vì sự bất tiện.","time":"' || to_char(NOW() - INTERVAL '11 days', 'YYYY-MM-DD HH24:MI') || '"}]',
    NOW() - INTERVAL '12 days'
FROM bookings b WHERE b.note LIKE '[DEMO-15]%';

-- ── 3. Report — đủ 3 chiều ───────────────────────────────────────────────
-- Customer → Owner (gắn booking DEMO-16, Thanh Khê thuộc owner1)
INSERT INTO reports (reporter_id, reportee_id, booking_id, stadium_id, category, description, status, created_at)
SELECT b.user_id, (SELECT user_id FROM owners WHERE owner_id = sc.owner_id), b.booking_id, b.stadium_id,
    'PROPERTY_DAMAGE', 'Mặt sân có nhiều chỗ rách lưới, tiềm ẩn nguy cơ chấn thương khi chơi.', 'OPEN', NOW() - INTERVAL '4 days'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
WHERE b.note LIKE '[DEMO-16]%';

-- Owner → Customer (gắn booking DEMO-17, Hàng Đẫy thuộc owner2)
INSERT INTO reports (reporter_id, reportee_id, booking_id, stadium_id, category, description, status, created_at)
SELECT (SELECT user_id FROM owners WHERE owner_id = sc.owner_id), b.user_id, b.booking_id, b.stadium_id,
    'NO_SHOW', 'Khách đặt sân nhưng không đến, cũng không báo hủy trước, gây thiệt hại doanh thu cho sân.', 'UNDER_REVIEW', NOW() - INTERVAL '3 days'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
WHERE b.note LIKE '[DEMO-17]%';

-- Joiner → Host (cộng đồng, match_id=2 "Giao lưu cầu lông đôi nam nữ", join_id=3 đã APPROVED)
INSERT INTO reports (reporter_id, reportee_id, match_request_id, join_request_id, category, description, status, created_at)
SELECT jr.user_id, mr.user_id, mr.match_id, jr.join_id,
    'HARASSMENT', 'Chủ kèo có lời lẽ thiếu tôn trọng trong nhóm chat sau khi trận đấu kết thúc.', 'OPEN', NOW() - INTERVAL '2 days'
FROM join_requests jr JOIN match_requests mr ON mr.match_id = jr.match_id
WHERE jr.join_id = 3;

-- Host → Joiner (cùng cặp match/join, chiều ngược lại — đã được Admin xem xét và bác bỏ)
INSERT INTO reports (reporter_id, reportee_id, match_request_id, join_request_id, category, description, status, resolved_by, resolved_at, resolution_note, created_at)
SELECT mr.user_id, jr.user_id, mr.match_id, jr.join_id,
    'OTHER', 'Người tham gia đến trễ 30 phút không báo trước, làm gián đoạn buổi giao lưu.',
    'DISMISSED', (SELECT user_id FROM users WHERE email = 'admin@sportvenue.com'), NOW() - INTERVAL '1 day',
    'Sự việc không đủ căn cứ để xử lý kỷ luật, nhắc nhở hai bên trao đổi rõ giờ giấc trước khi tham gia.',
    NOW() - INTERVAL '2 days'
FROM join_requests jr JOIN match_requests mr ON mr.match_id = jr.match_id
WHERE jr.join_id = 3;

-- ── 4. Review — gắn vào các booking COMPLETED của V115 (DEMO-10..17) ────────
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, v.rating, v.comment, v.owner_response, b.booking_date + INTERVAL '2 hours'
FROM bookings b
JOIN (VALUES
    ('[DEMO-10]%', 4, 'Sân đẹp, nhân viên nhiệt tình, sẽ quay lại.', NULL),
    ('[DEMO-11]%', 5, 'Sân cầu lông chuẩn thi đấu, ánh sáng tốt.', 'Cảm ơn bạn đã ủng hộ sân nhé!'),
    ('[DEMO-12]%', 3, 'Sân ổn nhưng bãi gửi xe hơi chật giờ cao điểm.', NULL),
    ('[DEMO-13]%', 5, 'Mặt cỏ mới, đá rất đã, giá hợp lý.', 'Rất vui vì bạn hài lòng, hẹn gặp lại!'),
    ('[DEMO-14]%', 4, 'Nhân viên hỗ trợ nhanh, sân sạch sẽ.', NULL),
    ('[DEMO-15]%', 4, 'Cầu lông ổn định, không bị trơn sàn.', NULL),
    ('[DEMO-16]%', 2, 'Lưới sân hơi cũ, cần bảo trì thêm.', 'Cảm ơn góp ý, sân sẽ thay lưới mới trong tuần này.'),
    ('[DEMO-17]%', 5, 'Sân vận động lớn, không khí thi đấu chuyên nghiệp.', NULL)
) AS v(note_pattern, rating, comment, owner_response) ON b.note LIKE v.note_pattern;
