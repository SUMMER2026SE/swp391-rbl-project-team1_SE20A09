-- Thêm DEPOSITED vào CHECK constraint của bảng bookings
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

ALTER TABLE bookings ADD CONSTRAINT bookings_payment_status_check CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED', 'DEPOSITED'));
