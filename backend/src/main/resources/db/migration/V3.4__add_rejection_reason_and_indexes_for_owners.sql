-- Bổ sung cột rejection_reason để lưu lý do Admin từ chối hồ sơ đối tác
ALTER TABLE owners ADD COLUMN rejection_reason VARCHAR(255);

-- Đánh index cột approved_status để tối ưu hóa truy vấn phân trang và lọc danh sách đối tác của Admin
CREATE INDEX IF NOT EXISTS idx_owners_approved_status ON owners(approved_status);

-- Đánh index cột user_id để tối ưu hóa việc kiểm tra thông tin đối tác khi login/upgrade tài khoản
CREATE INDEX IF NOT EXISTS idx_owners_user_id ON owners(user_id);
