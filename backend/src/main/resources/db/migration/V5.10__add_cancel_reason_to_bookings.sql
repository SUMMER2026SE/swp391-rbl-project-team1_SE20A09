-- UC-CUS-03: Lưu lý do khách hàng/owner hủy đơn đặt sân (nullable, tối đa 255 ký tự).
ALTER TABLE bookings ADD COLUMN cancel_reason VARCHAR(255);