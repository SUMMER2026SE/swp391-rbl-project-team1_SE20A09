-- Thêm cột service_fee vào bảng bookings
ALTER TABLE bookings ADD COLUMN service_fee NUMERIC(10,2);

-- Cập nhật hồi tố các đơn đặt sân cũ (với giá trị mặc định là 20000 VNĐ)
UPDATE bookings SET service_fee = 20000.00;

-- Thiết lập ràng buộc NOT NULL và giá trị mặc định cho cột mới
ALTER TABLE bookings ALTER COLUMN service_fee SET DEFAULT 0.00;
ALTER TABLE bookings ALTER COLUMN service_fee SET NOT NULL;
