-- Thêm cột cancel_reason vào bảng match_requests để lưu lý do hủy kèo của host
ALTER TABLE match_requests 
ADD COLUMN cancel_reason VARCHAR(255);
