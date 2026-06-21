-- UC-CUS-01: Recurring booking — thêm cột recurring_group_id để nhóm các đơn thuộc cùng chuỗi đặt sân định kỳ.
ALTER TABLE bookings
    ADD COLUMN recurring_group_id VARCHAR(36);

-- Partial index: chỉ index các dòng thuộc chuỗi (NULL = đặt đơn lẻ, không cần index).
-- Hỗ trợ truy vấn "tất cả đơn thuộc chuỗi X" nhanh cho Owner dashboard và cancel-the-series tương lai.
CREATE INDEX idx_bookings_recurring_group_id
    ON bookings(recurring_group_id)
    WHERE recurring_group_id IS NOT NULL;