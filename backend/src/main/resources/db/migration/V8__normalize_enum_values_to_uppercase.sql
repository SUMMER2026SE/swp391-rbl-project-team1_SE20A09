-- ══════════════════════════════════════════════════════════════════════════
-- V7__normalize_enum_values_to_uppercase.sql
-- Chuẩn hóa tất cả giá trị enum trong DB sang UPPERCASE
-- để khớp với Java enum constants (AVAILABLE, PENDING, APPROVED, ...)
-- ══════════════════════════════════════════════════════════════════════════

-- ── owners.approved_status ────────────────────────────────────────────────
ALTER TABLE owners DROP CONSTRAINT IF EXISTS owners_approved_status_check;
UPDATE owners SET approved_status = UPPER(approved_status);
ALTER TABLE owners ADD CONSTRAINT owners_approved_status_check
    CHECK (approved_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- ── stadiums.stadium_status ───────────────────────────────────────────────
ALTER TABLE stadiums DROP CONSTRAINT IF EXISTS stadiums_stadium_status_check;
UPDATE stadiums SET stadium_status = UPPER(stadium_status);
ALTER TABLE stadiums ADD CONSTRAINT stadiums_stadium_status_check
    CHECK (stadium_status IN ('AVAILABLE', 'MAINTENANCE', 'CLOSED'));

-- ── time_slots.slot_status ────────────────────────────────────────────────
ALTER TABLE time_slots DROP CONSTRAINT IF EXISTS time_slots_slot_status_check;
UPDATE time_slots SET slot_status = UPPER(slot_status);
ALTER TABLE time_slots ADD CONSTRAINT time_slots_slot_status_check
    CHECK (slot_status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE'));

-- ── bookings.booking_status ───────────────────────────────────────────────
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_booking_status_check;
UPDATE bookings SET booking_status = UPPER(booking_status);
ALTER TABLE bookings ADD CONSTRAINT bookings_booking_status_check
    CHECK (booking_status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));

-- ── bookings.payment_status ───────────────────────────────────────────────
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_payment_status_check;
UPDATE bookings SET payment_status = UPPER(payment_status);
ALTER TABLE bookings ADD CONSTRAINT bookings_payment_status_check
    CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED'));

-- ── payments.payment_status (TransactionStatus) ───────────────────────────
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_status_check;
UPDATE payments SET payment_status = UPPER(payment_status);
ALTER TABLE payments ADD CONSTRAINT payments_payment_status_check
    CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED'));

-- ── payments.payment_method ───────────────────────────────────────────────
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;
UPDATE payments SET payment_method = UPPER(payment_method);
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CASH', 'VNPAY', 'MOMO', 'BANKING'));

-- ── complaints.status ─────────────────────────────────────────────────────
-- V5 đã dùng UPPERCASE ('OPEN', 'IN_PROGRESS', 'RESOLVED') — không cần đổi

-- ── notifications.notification_type ──────────────────────────────────────
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;
UPDATE notifications SET notification_type = UPPER(notification_type);
ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN ('BOOKING', 'PAYMENT', 'PROMOTION', 'SYSTEM'));
