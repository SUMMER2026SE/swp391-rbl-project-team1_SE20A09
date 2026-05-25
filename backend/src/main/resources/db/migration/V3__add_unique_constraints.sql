-- ── Task 2: Add UNIQUE constraints for email and phone_number ──
-- Sử dụng DO block để kiểm tra và chỉ thêm nếu chưa tồn tại, tránh lỗi khi chạy migration.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.constraint_column_usage 
                   WHERE table_name = 'users' AND constraint_name = 'uq_users_email') THEN
        ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.constraint_column_usage 
                   WHERE table_name = 'users' AND constraint_name = 'uq_users_phone') THEN
        ALTER TABLE users ADD CONSTRAINT uq_users_phone UNIQUE (phone_number);
    END IF;
END $$;
