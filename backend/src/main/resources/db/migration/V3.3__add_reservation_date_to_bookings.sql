-- V3.3__add_reservation_date_to_bookings.sql
-- Thêm cột reservation_date để phân biệt với booking_date (ngày tạo đơn)
ALTER TABLE bookings ADD COLUMN reservation_date DATE;

-- Cập nhật reservation_date dựa trên booking_date hiện tại cho dữ liệu cũ
UPDATE bookings SET reservation_date = CAST(booking_date AS DATE) WHERE reservation_date IS NULL;

-- Ràng buộc NOT NULL sau khi đã cập nhật dữ liệu cũ
ALTER TABLE bookings ALTER COLUMN reservation_date SET NOT NULL;

-- Thêm index để tối ưu hóa việc lấy lịch sử theo ngày chơi
CREATE INDEX idx_bookings_reservation_date ON bookings(reservation_date);
