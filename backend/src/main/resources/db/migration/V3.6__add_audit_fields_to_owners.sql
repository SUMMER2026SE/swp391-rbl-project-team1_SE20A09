-- Bổ sung cột approved_by (FK đến users) và approved_at vào bảng owners để phục vụ Audit Trail
ALTER TABLE owners ADD COLUMN approved_by INTEGER;
ALTER TABLE owners ADD COLUMN approved_at TIMESTAMP;

-- Thêm khóa ngoại
ALTER TABLE owners ADD CONSTRAINT fk_owners_approved_by FOREIGN KEY (approved_by) REFERENCES users(user_id) ON DELETE SET NULL;
