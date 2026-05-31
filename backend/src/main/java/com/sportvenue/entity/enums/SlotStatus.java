package com.sportvenue.entity.enums;

/**
 * Trạng thái khung giờ (time slot) của sân.
 * Ánh xạ CHECK constraint: ('AVAILABLE', 'BOOKED', 'MAINTENANCE') trong bảng time_slots.
 */
public enum SlotStatus {
    AVAILABLE,    // Còn trống — có thể đặt
    BOOKED,       // Đã có người đặt
    MAINTENANCE   // Không nhận đặt do bảo trì
}
