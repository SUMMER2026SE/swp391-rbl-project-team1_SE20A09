-- Increase notification_type column length from 20 to 50 to accommodate longer notification type names
-- Examples: REFUND_EXCEPTION_DECISION (26 chars), COMPLAINT_OWNER_REPLIED (23 chars)

ALTER TABLE notifications
    ALTER COLUMN notification_type TYPE VARCHAR(50);
