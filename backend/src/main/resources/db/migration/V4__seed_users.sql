-- Seed Users
-- BCrypt hash of 'password123' is '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.'

-- Admin User
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status)
VALUES (
    (SELECT role_id FROM roles WHERE role_name = 'Admin'),
    'Admin',
    'System',
    '0900000001',
    'admin@sportvenue.com',
    '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=admin',
    100,
    'Gold',
    'Active'
) ON CONFLICT (email) DO NOTHING;

-- Owner User
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status)
VALUES (
    (SELECT role_id FROM roles WHERE role_name = 'Owner'),
    'Hoàng',
    'Mai Huy',
    '0900000002',
    'owner@sportvenue.com',
    '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=owner',
    50,
    'Silver',
    'Active'
) ON CONFLICT (email) DO NOTHING;

-- Owner Profile Link
INSERT INTO owners (user_id, business_name, tax_code, business_address, approved_status)
VALUES (
    (SELECT user_id FROM users WHERE email = 'owner@sportvenue.com'),
    'Sport Venue Owner Corp',
    'TAX-123456789',
    '123 Sports Road, District 1, HCMC',
    'APPROVED'
) ON CONFLICT (user_id) DO NOTHING;

-- Customer User
INSERT INTO users (role_id, first_name, last_name, phone_number, email, password_hash, avatar_url, user_point, user_rank, account_status)
VALUES (
    (SELECT role_id FROM roles WHERE role_name = 'Customer'),
    'Hoàng',
    'Mai Huy',
    '0912345678',
    'customer@sportvenue.com',
    '$2a$10$dY/hQd0G/jQoN5uJ9V2OSe2P5xM2lB6e6gN3xVwz1H9h1dE7I5bK.',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=customer',
    0,
    'Bronze',
    'Active'
) ON CONFLICT (email) DO NOTHING;
