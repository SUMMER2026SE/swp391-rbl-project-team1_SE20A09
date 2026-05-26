-- ══════════════════════════════════════════════════════════════════════════
-- V2__add_otp_tokens.sql
-- Thêm cột is_verified vào users và bảng otp_tokens cho xác thực email
-- ══════════════════════════════════════════════════════════════════════════

-- Thêm cột is_verified vào bảng users (mặc định false — chưa xác thực email)
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Bảng lưu OTP để xác thực email
CREATE TABLE otp_tokens (
    otp_id      SERIAL PRIMARY KEY,
    user_id     INT             NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    otp_code    VARCHAR(6)      NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    used        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_tokens_user_id ON otp_tokens(user_id);
CREATE INDEX idx_otp_tokens_expires_at ON otp_tokens(expires_at);
