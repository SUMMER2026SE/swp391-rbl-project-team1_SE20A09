-- Thêm UNIQUE constraint: Mỗi customer chỉ được có 1 review cho mỗi venue(stadium)
-- Thỏa mãn yêu cầu: "Mỗi customer chỉ 1 review/venue" (V4.2)
ALTER TABLE reviews ADD CONSTRAINT unique_user_stadium_review UNIQUE (user_id, stadium_id);
