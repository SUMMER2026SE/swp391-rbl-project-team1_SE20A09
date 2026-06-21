-- ══════════════════════════════════════════════════════════════════════════
-- V4_5__fix_owner_status_and_seed_rejected.sql
-- Cập nhật đa dạng trạng thái cho 3 tài khoản Pending để mô phỏng thực tế
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Khánh Lê Minh (pending_owner@sportvenue.com) -> Bị từ chối (REJECTED / BLOCKED)
UPDATE users 
SET account_status = 'BLOCKED', lock_reason = 'Hồ sơ bị từ chối: Giấy phép kinh doanh không hợp lệ' 
WHERE email = 'pending_owner@sportvenue.com';

UPDATE owners 
SET approved_status = 'REJECTED' 
WHERE user_id = (SELECT user_id FROM users WHERE email = 'pending_owner@sportvenue.com');

-- 2. Anh Nguyễn Nâng Cấp (customer.upgrade@example.com) -> Đã được duyệt (APPROVED / ACTIVE)
UPDATE users 
SET account_status = 'ACTIVE', lock_reason = NULL 
WHERE email = 'customer.upgrade@example.com';

UPDATE owners 
SET approved_status = 'APPROVED' 
WHERE user_id = (SELECT user_id FROM users WHERE email = 'customer.upgrade@example.com');

-- 3. Bình Trần Chủ Sân (owner.pending@example.com) -> Đang chờ duyệt (PENDING / PENDING)
UPDATE users 
SET account_status = 'PENDING', lock_reason = NULL 
WHERE email = 'owner.pending@example.com';

UPDATE owners 
SET approved_status = 'PENDING' 
WHERE user_id = (SELECT user_id FROM users WHERE email = 'owner.pending@example.com');
