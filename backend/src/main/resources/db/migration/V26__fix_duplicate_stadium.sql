-- ══════════════════════════════════════════════════════════════════════════
-- V16__fix_duplicate_stadium.sql
-- Xóa sân và đơn đặt sân "Sân Bóng Đá Mỹ Đình" do hệ thống tạo (bị trùng với sân user tự tạo).
-- Đảm bảo không có 2 sân trùng tên và không bị trùng giờ.
-- ══════════════════════════════════════════════════════════════════════════

-- 1. Xóa các Payment liên quan đến sân do system tạo (địa chỉ Nam Từ Liêm, Hà Nội)
DELETE FROM payments WHERE booking_id IN (
    SELECT booking_id FROM bookings b
    JOIN stadiums s ON b.stadium_id = s.stadium_id
    WHERE s.stadium_name = 'Sân Bóng Đá Mỹ Đình' AND s.address = 'Nam Từ Liêm, Hà Nội'
);

-- 2. Xóa các Booking liên quan đến sân do system tạo
DELETE FROM bookings WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình' AND address = 'Nam Từ Liêm, Hà Nội'
);

-- 3. Xóa các Time Slots của sân do system tạo
DELETE FROM time_slots WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình' AND address = 'Nam Từ Liêm, Hà Nội'
);

-- 4. Xóa Ảnh Sân
DELETE FROM stadium_images WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình' AND address = 'Nam Từ Liêm, Hà Nội'
);

-- 5. Xóa chính Sân đó
DELETE FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình' AND address = 'Nam Từ Liêm, Hà Nội';
