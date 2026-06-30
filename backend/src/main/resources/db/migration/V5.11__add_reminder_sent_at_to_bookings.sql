-- V5.11__add_reminder_sent_at_to_bookings.sql
-- Thêm cột reminder_sent_at để BookingReminderScheduler đánh dấu đã nhắc lịch
-- NULL = chưa nhắc, NOT NULL = đã nhắc (chống duplicate notification)

ALTER TABLE bookings
    ADD COLUMN reminder_sent_at TIMESTAMP;

-- Partial index: chỉ index các booking CONFIRMED chưa nhắc, tối ưu query scheduler
-- Lưu ý: booking_status phải được lưu dạng VARCHAR 'CONFIRMED' (EnumType.STRING)
CREATE INDEX idx_bookings_reminder_pending
    ON bookings (reservation_date)
    WHERE booking_status = 'CONFIRMED'
      AND reminder_sent_at IS NULL;
