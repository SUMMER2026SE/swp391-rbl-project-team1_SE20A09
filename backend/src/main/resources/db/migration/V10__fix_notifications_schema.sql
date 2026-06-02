-- V10__fix_notifications_schema.sql
-- Refactor notifications table schema to be central (tied to user_id)

-- Drop old table if exists
DROP TABLE IF EXISTS notifications CASCADE;

-- Create notifications table with correct structure
CREATE TABLE notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    notification_type VARCHAR(20) NOT NULL
        CHECK (notification_type IN ('BOOKING', 'PAYMENT', 'PROMOTION', 'SYSTEM', 'REVIEW', 'COMPLAINT')),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_resource_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_is_read ON notifications(user_id, is_read);
