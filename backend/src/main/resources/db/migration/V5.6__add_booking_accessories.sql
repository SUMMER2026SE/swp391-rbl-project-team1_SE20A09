-- UC-CUS-01: Bảng phụ kiện kèm theo booking — ghi lại từng accessory khách chọn kèm sân.
-- unit_price được chốt tại thời điểm tạo booking (snapshot giá) — bảo vệ lịch sử giá.
CREATE TABLE booking_accessories (
    id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    accessory_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_booking_accessories_booking_id ON booking_accessories(booking_id);
CREATE INDEX idx_booking_accessories_accessory_id ON booking_accessories(accessory_id);
