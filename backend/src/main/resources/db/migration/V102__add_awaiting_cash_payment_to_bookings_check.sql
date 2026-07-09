-- Thêm AWAITING_CASH_PAYMENT vào CHECK constraint của bookings.payment_status
-- (enum PaymentStatus đã có giá trị này nhưng constraint DB chưa theo kịp,
-- khiến mọi lần set giá trị này đều bị Postgres từ chối ở tầng DB).
DO $$
DECLARE
    const_name text;
BEGIN
    SELECT constraint_name INTO const_name
    FROM information_schema.constraint_column_usage
    WHERE table_name = 'bookings' AND column_name = 'payment_status'
    LIMIT 1;

    IF const_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE bookings DROP CONSTRAINT %I', const_name);
    END IF;
END $$;

ALTER TABLE bookings ADD CONSTRAINT bookings_payment_status_check
    CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED', 'DEPOSITED', 'AWAITING_CASH_PAYMENT'));
