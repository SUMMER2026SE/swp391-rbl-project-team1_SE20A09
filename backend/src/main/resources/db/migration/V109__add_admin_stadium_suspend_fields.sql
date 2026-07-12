ALTER TABLE stadiums
    ADD COLUMN admin_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN admin_suspended_reason TEXT,
    ADD COLUMN admin_suspended_at TIMESTAMP;

CREATE INDEX idx_stadiums_admin_suspended ON stadiums(admin_suspended);
