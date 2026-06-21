-- ══════════════════════════════════════════════════════════════════════════
-- V3.5__seed_owner_registration_test_data.sql
-- Seed pending owner registrations and customer upgrade requests for testing UC-ADM-06
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Create a Customer Upgrade Request (User exists as Customer, has Owner profile PENDING)
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status, is_verified)
VALUES
    ((SELECT role_id FROM roles WHERE role_name = 'Customer'), 'Anh', 'Nguyễn Nâng Cấp', '0922222222', 'customer.upgrade@example.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=upgrade', 10, 'BRONZE', 'ACTIVE', TRUE);

INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES
    ((SELECT user_id FROM users WHERE email = 'customer.upgrade@example.com'), 'Hộ Kinh Doanh Thành Công Sport', '1102938475', '789 Sư Vạn Hạnh, Quận 10, TP.HCM', 'PENDING');


-- ── 2. Create a Direct Owner Registration Request (User exists as Owner but account is PENDING, has Owner profile PENDING)
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status, is_verified)
VALUES
    ((SELECT role_id FROM roles WHERE role_name = 'Owner'), 'Bình', 'Trần Chủ Sân', '0933333333', 'owner.pending@example.com',
     '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.', 'https://api.dicebear.com/7.x/adventurer/svg?seed=ownerpending', 0, 'BRONZE', 'PENDING', TRUE);

INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES
    ((SELECT user_id FROM users WHERE email = 'owner.pending@example.com'), 'Công Ty TNHH Thể Thao Bình Minh', '9988776655', '321 Điện Biên Phủ, Bình Thạnh, TP.HCM', 'PENDING');
