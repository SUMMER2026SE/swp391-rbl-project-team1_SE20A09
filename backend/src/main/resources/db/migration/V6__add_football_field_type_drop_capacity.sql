-- ══════════════════════════════════════════════════════════════════════════
-- V6__add_football_field_type_drop_capacity.sql
-- Author  : Team SportVenue
-- Scope   : 
--   (1) Thêm cột football_field_type vào stadiums
--   (2) Xóa cột capacity khỏi stadiums
--   (3) Thêm cột is_football_type vào sport_types
-- ══════════════════════════════════════════════════════════════════════════
-- ── STADIUMS ──────────────────────────────────────────────────────────────
-- Thêm football_field_type (nullable — null cho môn không phải bóng đá)
ALTER TABLE stadiums
    ADD COLUMN football_field_type VARCHAR(20)
        CHECK (football_field_type IN ('FIVE_A_SIDE', 'SEVEN_A_SIDE', 'ELEVEN_A_SIDE', 'FUTSAL'));
-- Xóa capacity (không còn dùng trong nghiệp vụ)
ALTER TABLE stadiums DROP COLUMN capacity;
-- ── SPORT_TYPES ───────────────────────────────────────────────────────────
-- Thêm flag is_football_type để frontend nhận diện môn bóng đá
ALTER TABLE sport_types
    ADD COLUMN is_football_type BOOLEAN NOT NULL DEFAULT FALSE;
-- Auto-update: sport_code bắt đầu bằng FOOTBALL → đánh dấu football
UPDATE sport_types
SET is_football_type = TRUE
WHERE sport_code LIKE 'FOOTBALL%';
