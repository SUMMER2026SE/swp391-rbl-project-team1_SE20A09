CREATE TABLE account_status_history (
    history_id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    changed_by INTEGER,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_status_history_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_account_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users(user_id),
    CONSTRAINT chk_account_status_history_previous_status
        CHECK (previous_status IN ('PENDING', 'ACTIVE', 'BLOCKED')),
    CONSTRAINT chk_account_status_history_new_status
        CHECK (new_status IN ('PENDING', 'ACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_account_status_history_user
    ON account_status_history(user_id, created_at DESC);
CREATE INDEX idx_account_status_history_changed_by
    ON account_status_history(changed_by);

ALTER TABLE notifications DROP CONSTRAINT notifications_notification_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN (
        'BOOKING', 'PAYMENT', 'PROMOTION', 'SYSTEM', 'REVIEW', 'COMPLAINT',
        'OWNER_APPROVAL', 'STADIUM_APPROVAL', 'ACCOUNT_LOCK', 'APPEAL', 'REPORT'
    ));
