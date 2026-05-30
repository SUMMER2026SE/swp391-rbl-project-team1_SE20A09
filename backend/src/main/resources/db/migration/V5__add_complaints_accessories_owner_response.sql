-- ══════════════════════════════════════════════════════════════════════════
-- V5__add_complaints_accessories_owner_response.sql
-- Thêm bảng complaints, accessories và cột owner_response vào reviews
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Complaints — Khiếu nại của khách hàng ──────────────────────────────
CREATE TABLE complaints (
    complaint_id    SERIAL PRIMARY KEY,
    booking_id      INT             NOT NULL REFERENCES bookings(booking_id),
    user_id         INT             NOT NULL REFERENCES users(user_id),
    content         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    response        TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_complaints_booking_id ON complaints(booking_id);
CREATE INDEX idx_complaints_user_id    ON complaints(user_id);
CREATE INDEX idx_complaints_status     ON complaints(status);

-- ── 2. Accessories — Phụ kiện cho thuê kèm sân ────────────────────────────
CREATE TABLE accessories (
    accessory_id    SERIAL PRIMARY KEY,
    stadium_id      INT             NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    price_per_unit  DECIMAL(10, 2)  NOT NULL,
    quantity        INT             NOT NULL DEFAULT 0,
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_accessories_stadium_id ON accessories(stadium_id);

-- ── 3. Thêm cột owner_response vào reviews ────────────────────────────────
-- Cho phép Owner phản hồi đánh giá của khách hàng (UC-OWN-08)
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS owner_response TEXT;
