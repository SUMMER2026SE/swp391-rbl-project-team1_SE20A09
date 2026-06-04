-- ══════════════════════════════════════════════════════════════════════════
-- V20__seed_reviews_for_thanh_cong.sql
-- Thêm dữ liệu đánh giá thực tế cho Sân bóng Thành Công để hiển thị trên UI.
-- ══════════════════════════════════════════════════════════════════════════

INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT 
    b.booking_id,
    b.user_id,
    b.stadium_id,
    1,
    'Sân tệ, Như sịt',
    'tại sao ạ',
    CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM bookings b
JOIN stadiums s ON b.stadium_id = s.stadium_id
WHERE s.stadium_name = 'Sân bóng Thành Công'
AND b.booking_status = 'COMPLETED'
AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.booking_id = b.booking_id);
