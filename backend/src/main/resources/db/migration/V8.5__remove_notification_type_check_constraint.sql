-- Simply remove the old check constraint that was too restrictive
-- The VARCHAR(50) column is enough for validation at the application layer
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;
