-- ══════════════════════════════════════════════════════════════════════════
-- V5.2__expand_sport_types.sql — Expand sport_types table for UC-ADM-07
-- ══════════════════════════════════════════════════════════════════════════

ALTER TABLE sport_types
ADD COLUMN sport_code VARCHAR(20) UNIQUE,
ADD COLUMN name_en VARCHAR(50),
ADD COLUMN icon VARCHAR(10),
ADD COLUMN description TEXT,
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing data with codes (simple slug of name)
UPDATE sport_types SET sport_code = UPPER(REPLACE(sport_name, ' ', '_')) WHERE sport_code IS NULL;
UPDATE sport_types SET name_en = sport_name WHERE name_en IS NULL;

-- Make sport_code NOT NULL after updating existing rows
ALTER TABLE sport_types ALTER COLUMN sport_code SET NOT NULL;
