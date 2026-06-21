-- UC-CUS-01: Real backend hold — booking mới tạo được giữ 5 phút chờ thanh toán.
-- 1) Thêm cột expired_at (TIMESTAMP, nullable — NULL nghĩa là không còn trong trạng thái PENDING_PAYMENT).
ALTER TABLE bookings
    ADD COLUMN expired_at TIMESTAMP;

-- 2) Index cho scheduler quét nhanh các booking PENDING_PAYMENT đã quá hạn.
CREATE INDEX idx_bookings_pending_payment_expiry
    ON bookings(expired_at)
    WHERE booking_status = 'PENDING_PAYMENT' AND expired_at IS NOT NULL;

-- 3) Cập nhật CHECK constraint để cho phép 'PENDING_PAYMENT'.
-- Postgres auto-named CHECK constraints as 'bookings_booking_status_check' from V1 init.
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'bookings'
      AND pg_get_constraintdef(con.oid) LIKE '%booking_status%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE bookings DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE bookings ADD CONSTRAINT bookings_booking_status_check
    CHECK (booking_status IN ('PENDING_PAYMENT', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));
