-- ══════════════════════════════════════════════════════════════════════════
-- V18__delete_my_dinh_stadium.sql
-- Xóa hoàn toàn Sân Bóng Đá Mỹ Đình theo yêu cầu.
-- ══════════════════════════════════════════════════════════════════════════

DELETE FROM payments WHERE booking_id IN (
    SELECT booking_id FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM reviews WHERE booking_id IN (
    SELECT booking_id FROM bookings b JOIN stadiums s ON b.stadium_id = s.stadium_id WHERE s.stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM bookings WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM time_slots WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM stadium_images WHERE stadium_id IN (
    SELECT stadium_id FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình'
);
DELETE FROM stadiums WHERE stadium_name = 'Sân Bóng Đá Mỹ Đình';
