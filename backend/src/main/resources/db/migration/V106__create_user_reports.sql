CREATE TABLE reports (
    report_id          SERIAL PRIMARY KEY,
    reporter_id        INTEGER      NOT NULL REFERENCES users(user_id),
    reportee_id        INTEGER      NOT NULL REFERENCES users(user_id),
    booking_id         INTEGER      REFERENCES bookings(booking_id),
    match_request_id   INTEGER      REFERENCES match_requests(match_id),
    join_request_id    INTEGER      REFERENCES join_requests(join_id),
    stadium_id         INTEGER      REFERENCES stadiums(stadium_id),
    category           VARCHAR(30)  NOT NULL,
    description        TEXT         NOT NULL,
    status             VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    resolved_by        INTEGER      REFERENCES users(user_id),
    resolved_at        TIMESTAMP,
    resolution_note    TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reports_category CHECK (category IN (
        'NO_SHOW', 'PROPERTY_DAMAGE', 'HARASSMENT', 'FRAUD',
        'PAYMENT_ABUSE', 'FAKE_LISTING', 'OTHER'
    )),
    CONSTRAINT chk_reports_status CHECK (status IN (
        'OPEN', 'UNDER_REVIEW', 'ACTION_TAKEN', 'DISMISSED'
    )),
    CONSTRAINT chk_reports_not_self CHECK (reporter_id <> reportee_id),
    CONSTRAINT chk_reports_has_context CHECK (
        booking_id IS NOT NULL OR match_request_id IS NOT NULL OR join_request_id IS NOT NULL
    )
);

CREATE TABLE report_evidence_urls (
    report_id      INTEGER      NOT NULL REFERENCES reports(report_id) ON DELETE CASCADE,
    evidence_url   VARCHAR(500) NOT NULL,
    PRIMARY KEY (report_id, evidence_url)
);

CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_reports_reportee_id ON reports(reportee_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_category ON reports(category);
CREATE INDEX idx_reports_created_at ON reports(created_at);
CREATE INDEX idx_reports_booking_id ON reports(booking_id);
CREATE INDEX idx_reports_match_request_id ON reports(match_request_id);
CREATE INDEX idx_reports_join_request_id ON reports(join_request_id);
