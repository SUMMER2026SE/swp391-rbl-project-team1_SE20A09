package com.sportvenue.entity.enums;

/**
 * Trạng thái đơn đặt sân.
 * Ánh xạ CHECK constraint: ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED') trong bảng bookings.
 */
public enum BookingStatus {
    PENDING,    // Chờ Owner xác nhận
    CONFIRMED,  // Owner đã xác nhận
    COMPLETED,  // Đã hoàn thành (sau giờ chơi)
    CANCELLED   // Đã hủy
}
