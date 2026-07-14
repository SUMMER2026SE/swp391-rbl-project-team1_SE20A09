-- Add review_reminder_sent_at column to bookings table
-- Used by ReviewReminderScheduler to track which bookings have had review reminders sent.
-- NULL = not yet sent, NOT NULL = reminder already sent (prevents duplicates).
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS review_reminder_sent_at TIMESTAMP;
