-- Migration V105: Tạo bảng refund_exception_requests cho luồng xét duyệt ngoại lệ hoàn tiền (Mục 1.6 P0)
-- Áp dụng khi khách hủy <12h được hoàn 0% nhưng có lý do bất khả kháng và muốn xin xem xét lại.

CREATE TABLE refund_exception_requests (
    request_id          SERIAL PRIMARY KEY,
    booking_id          INTEGER      NOT NULL REFERENCES bookings(booking_id),
    customer_id         INTEGER      NOT NULL REFERENCES users(user_id),
    reason              TEXT         NOT NULL,
    evidence_url        VARCHAR(500),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_OWNER',
    owner_note          TEXT,
    admin_note          TEXT,
    refund_percent      INTEGER,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    owner_reviewed_at   TIMESTAMP,
    admin_reviewed_at   TIMESTAMP,
    expires_at          TIMESTAMP    NOT NULL,
    CONSTRAINT chk_refund_exception_status CHECK (status IN (
        'PENDING_OWNER', 'APPROVED_OWNER', 'REJECTED_OWNER',
        'PENDING_ADMIN', 'APPROVED_ADMIN', 'REJECTED_ADMIN', 'EXPIRED'
    )),
    CONSTRAINT chk_refund_exception_percent CHECK (
        refund_percent IS NULL OR refund_percent IN (50, 100)
    )
);

CREATE INDEX idx_refund_exception_booking ON refund_exception_requests(booking_id);
CREATE INDEX idx_refund_exception_status  ON refund_exception_requests(status);
CREATE INDEX idx_refund_exception_customer ON refund_exception_requests(customer_id);
