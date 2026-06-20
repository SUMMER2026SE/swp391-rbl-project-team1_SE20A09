-- UC-CUS-01: Partial unique index — chỉ một booking ACTIVE cho mỗi (stadium, slot, reservationDate).
-- Trạng thái ACTIVE = PENDING | CONFIRMED (CANCELLED / COMPLETED không chiếm slot).
-- Bổ sung tầng bảo vệ cơ sở dữ liệu ngoài @Lock(PESSIMISTIC_WRITE) trong service.
CREATE UNIQUE INDEX uq_active_bookings_per_slot
    ON bookings (stadium_id, slot_id, reservation_date)
    WHERE booking_status IN ('PENDING', 'CONFIRMED');
