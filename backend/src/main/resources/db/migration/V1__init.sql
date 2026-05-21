-- ══════════════════════════════════════════════════════════════════════════
-- V1__init.sql — Sport Venue Management System
-- Database Design by Team 1 — SE20A09
-- Converted from SQL Server → PostgreSQL 16
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Roles ──────────────────────────────────────────────────────────────
CREATE TABLE roles (
    role_id     SERIAL PRIMARY KEY,
    role_name   VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO roles (role_name) VALUES ('Admin'), ('Owner'), ('Customer');

-- ── 2. SportTypes ─────────────────────────────────────────────────────────
CREATE TABLE sport_types (
    sport_type_id   SERIAL PRIMARY KEY,
    sport_name      VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO sport_types (sport_name) VALUES
    ('Football'), ('Badminton'), ('Basketball'), ('Tennis'), ('Volleyball');

-- ── 3. Promotions ─────────────────────────────────────────────────────────
CREATE TABLE promotions (
    promotion_id        SERIAL PRIMARY KEY,
    promo_code          VARCHAR(20)     NOT NULL UNIQUE,
    description         TEXT,
    discount_percent    DECIMAL(5, 2),
    max_discount_amount DECIMAL(10, 2),
    start_date          TIMESTAMP       NOT NULL,
    end_date            TIMESTAMP       NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ── 4. Users ──────────────────────────────────────────────────────────────
CREATE TABLE users (
    user_id         SERIAL PRIMARY KEY,
    role_id         INT             NOT NULL REFERENCES roles(role_id),
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    phone_number    VARCHAR(15)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    avatar_url      VARCHAR(255),
    user_point      INT             NOT NULL DEFAULT 0,
    user_rank       VARCHAR(20)     NOT NULL DEFAULT 'Bronze'
                        CHECK (user_rank IN ('Bronze', 'Silver', 'Gold', 'Diamond')),
    account_status  VARCHAR(20)     NOT NULL DEFAULT 'Active'
                        CHECK (account_status IN ('Active', 'Blocked', 'Pending')),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 5. Owners ─────────────────────────────────────────────────────────────
CREATE TABLE owners (
    owner_id            SERIAL PRIMARY KEY,
    user_id             INT             NOT NULL UNIQUE REFERENCES users(user_id),
    business_name       VARCHAR(100),
    tax_code            VARCHAR(30),
    business_address    TEXT,
    approved_status     VARCHAR(20)     NOT NULL DEFAULT 'Pending'
                            CHECK (approved_status IN ('Pending', 'Approved', 'Rejected')),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 6. Stadiums ───────────────────────────────────────────────────────────
CREATE TABLE stadiums (
    stadium_id      SERIAL PRIMARY KEY,
    owner_id        INT             NOT NULL REFERENCES owners(owner_id),
    sport_type_id   INT             NOT NULL REFERENCES sport_types(sport_type_id),
    stadium_name    VARCHAR(100)    NOT NULL,
    description     TEXT,
    address         TEXT            NOT NULL,
    price_per_hour  DECIMAL(10, 2)  NOT NULL,
    capacity        INT,
    open_time       TIME,
    close_time      TIME,
    stadium_status  VARCHAR(20)     NOT NULL DEFAULT 'Available'
                        CHECK (stadium_status IN ('Available', 'Maintenance', 'Closed')),
    average_rating  DECIMAL(3, 2)   NOT NULL DEFAULT 5.0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 7. StadiumImages ──────────────────────────────────────────────────────
CREATE TABLE stadium_images (
    image_id    SERIAL PRIMARY KEY,
    stadium_id  INT             NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    image_url   VARCHAR(255)    NOT NULL,
    uploaded_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 8. TimeSlots ──────────────────────────────────────────────────────────
CREATE TABLE time_slots (
    slot_id         SERIAL PRIMARY KEY,
    stadium_id      INT             NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,
    slot_status     VARCHAR(20)     NOT NULL DEFAULT 'Available'
                        CHECK (slot_status IN ('Available', 'Booked', 'Maintenance'))
);

-- ── 9. Bookings ───────────────────────────────────────────────────────────
CREATE TABLE bookings (
    booking_id      SERIAL PRIMARY KEY,
    user_id         INT             NOT NULL REFERENCES users(user_id),
    stadium_id      INT             NOT NULL REFERENCES stadiums(stadium_id),
    slot_id         INT             NOT NULL REFERENCES time_slots(slot_id),
    total_price     DECIMAL(10, 2)  NOT NULL,
    booking_status  VARCHAR(20)     NOT NULL DEFAULT 'Pending'
                        CHECK (booking_status IN ('Pending', 'Confirmed', 'Completed', 'Cancelled')),
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'Unpaid'
                        CHECK (payment_status IN ('Unpaid', 'Paid', 'Refunded')),
    booking_date    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            TEXT
);

-- ── 10. Payments ──────────────────────────────────────────────────────────
CREATE TABLE payments (
    payment_id      SERIAL PRIMARY KEY,
    booking_id      INT             NOT NULL REFERENCES bookings(booking_id),
    payment_method  VARCHAR(20)     NOT NULL
                        CHECK (payment_method IN ('Cash', 'VNPay', 'Momo', 'Banking')),
    amount          DECIMAL(10, 2)  NOT NULL,
    transaction_code VARCHAR(100),
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'Pending'
                        CHECK (payment_status IN ('Pending', 'Success', 'Failed')),
    paid_at         TIMESTAMP
);

-- ── 11. Reviews ───────────────────────────────────────────────────────────
CREATE TABLE reviews (
    review_id       SERIAL PRIMARY KEY,
    booking_id      INT             NOT NULL UNIQUE REFERENCES bookings(booking_id),
    user_id         INT             NOT NULL REFERENCES users(user_id),
    stadium_id      INT             NOT NULL REFERENCES stadiums(stadium_id),
    rating_score    INT             NOT NULL CHECK (rating_score BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 12. Notifications ─────────────────────────────────────────────────────
CREATE TABLE notifications (
    notification_id     SERIAL PRIMARY KEY,
    user_id             INT             NOT NULL REFERENCES users(user_id),
    title               VARCHAR(255)    NOT NULL,
    content             TEXT            NOT NULL,
    notification_type   VARCHAR(20)
                            CHECK (notification_type IN ('Booking', 'Payment', 'Promotion', 'System')),
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 13. Conversations ─────────────────────────────────────────────────────
CREATE TABLE conversations (
    conversation_id SERIAL PRIMARY KEY,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 14. ConversationMembers ───────────────────────────────────────────────
CREATE TABLE conversation_members (
    conversation_id INT         NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    user_id         INT         NOT NULL REFERENCES users(user_id),
    joined_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_conversation_members PRIMARY KEY (conversation_id, user_id)
);

-- ── 15. Messages ──────────────────────────────────────────────────────────
CREATE TABLE messages (
    message_id      SERIAL PRIMARY KEY,
    conversation_id INT         NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    sender_id       INT         NOT NULL REFERENCES users(user_id),
    message_content TEXT        NOT NULL,
    sent_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 16. Posts ─────────────────────────────────────────────────────────────
CREATE TABLE posts (
    post_id     SERIAL PRIMARY KEY,
    user_id     INT             NOT NULL REFERENCES users(user_id),
    title       VARCHAR(255),
    content     TEXT            NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 17. Comments ──────────────────────────────────────────────────────────
CREATE TABLE comments (
    comment_id      SERIAL PRIMARY KEY,
    post_id         INT         NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id         INT         NOT NULL REFERENCES users(user_id),
    comment_content TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 18. Booking_Promotions ────────────────────────────────────────────────
CREATE TABLE booking_promotions (
    booking_id      INT NOT NULL REFERENCES bookings(booking_id),
    promotion_id    INT NOT NULL REFERENCES promotions(promotion_id),
    CONSTRAINT pk_booking_promotions PRIMARY KEY (booking_id, promotion_id)
);

-- ══════════════════════════════════════════════════════════════════════════
-- Indexes — tối ưu các truy vấn thường gặp
-- ══════════════════════════════════════════════════════════════════════════
CREATE INDEX idx_users_email          ON users(email);
CREATE INDEX idx_users_role_id        ON users(role_id);
CREATE INDEX idx_stadiums_owner_id    ON stadiums(owner_id);
CREATE INDEX idx_stadiums_sport_type  ON stadiums(sport_type_id);
CREATE INDEX idx_stadiums_status      ON stadiums(stadium_status);
CREATE INDEX idx_time_slots_stadium   ON time_slots(stadium_id);
CREATE INDEX idx_time_slots_status    ON time_slots(slot_status);
CREATE INDEX idx_bookings_user_id     ON bookings(user_id);
CREATE INDEX idx_bookings_stadium_id  ON bookings(stadium_id);
CREATE INDEX idx_bookings_status      ON bookings(booking_status);
CREATE INDEX idx_messages_conv_id     ON messages(conversation_id);
CREATE INDEX idx_posts_user_id        ON posts(user_id);
CREATE INDEX idx_notifications_user   ON notifications(user_id, is_read);
