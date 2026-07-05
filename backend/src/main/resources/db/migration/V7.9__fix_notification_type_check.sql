-- NotificationType enum was extended with OWNER_APPROVAL and STADIUM_APPROVAL
-- but the DB check constraint (from V1__init_schema.sql) was never updated,
-- causing every owner-registration / stadium-approval notification insert to fail.
ALTER TABLE notifications DROP CONSTRAINT notifications_notification_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN (
        'BOOKING', 'PAYMENT', 'PROMOTION', 'SYSTEM', 'REVIEW', 'COMPLAINT',
        'OWNER_APPROVAL', 'STADIUM_APPROVAL'
    ));
