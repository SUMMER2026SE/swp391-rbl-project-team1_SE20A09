-- ══════════════════════════════════════════════════════════════════════════
-- V21__fix_duplicate_reviews.sql
-- Xóa các đánh giá trùng lặp do seed data, chỉ giữ lại 1 cái duy nhất cho Sân bóng Thành Công.
-- ══════════════════════════════════════════════════════════════════════════

DELETE FROM reviews 
WHERE comment = 'Sân tệ, Như sịt' 
AND review_id NOT IN (
    SELECT MIN(review_id) 
    FROM reviews 
    WHERE comment = 'Sân tệ, Như sịt'
);
