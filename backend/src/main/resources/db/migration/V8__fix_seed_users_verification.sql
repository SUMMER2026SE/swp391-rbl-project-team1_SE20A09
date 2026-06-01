-- ══════════════════════════════════════════════════════════════════════════
-- V8__fix_seed_users_verification.sql
-- Khắc phục lỗi logic seed dữ liệu:
-- Toàn bộ tài khoản dùng để test (seeding) phải ở trạng thái 'ACTIVE' và 'is_verified = TRUE'
-- để tránh bị chặn đăng nhập bắt buộc phải OTP verify.
-- ══════════════════════════════════════════════════════════════════════════

UPDATE users 
SET is_verified = TRUE, account_status = 'Active'
WHERE email IN (
    'admin@sportvenue.com',
    'owner@sportvenue.com',
    'customer@sportvenue.com'
);
