ALTER TABLE reports DROP CONSTRAINT IF EXISTS chk_reports_has_context;

ALTER TABLE reports ADD CONSTRAINT chk_reports_has_context CHECK (
    booking_id IS NOT NULL
        OR match_request_id IS NOT NULL
        OR join_request_id IS NOT NULL
        OR stadium_id IS NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_stadium_id ON reports(stadium_id);
