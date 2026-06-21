-- Thêm trường lock_reason vào bảng users để lưu lý do khóa tài khoản
ALTER TABLE users ADD COLUMN IF NOT EXISTS lock_reason TEXT;
