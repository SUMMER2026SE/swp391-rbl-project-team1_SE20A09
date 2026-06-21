-- UC-CUS-03: Thêm cột cancel_reason để lưu lý do khách hàng/chủ sân hủy đơn đặt sân.
ALTER TABLE bookings ADD COLUMN cancel_reason VARCHAR(255);
