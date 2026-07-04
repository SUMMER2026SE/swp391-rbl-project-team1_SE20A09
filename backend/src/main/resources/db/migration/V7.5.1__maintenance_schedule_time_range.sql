-- Mở rộng MaintenanceSchedule từ "chỉ chặn nguyên ngày" sang hỗ trợ khung giờ cụ thể.
-- NULL ở cả 2 cột = giữ nguyên hành vi cũ (chặn cả ngày/nhiều ngày).
-- start_time/end_time kết hợp với start_date/end_date tạo thành 1 khoảng thời gian LIÊN TỤC
-- (VD start_date=hôm nay start_time=04:00, end_date=ngày mai end_time=09:00 -> xuyên đêm),
-- KHÔNG phải khung giờ lặp lại mỗi ngày. Validate thứ tự start < end thực hiện ở service layer
-- vì phụ thuộc cả start_date lẫn end_date.
ALTER TABLE maintenance_schedules
    ADD COLUMN start_time TIME,
    ADD COLUMN end_time TIME;
