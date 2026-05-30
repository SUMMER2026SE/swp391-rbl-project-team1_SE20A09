package com.sportvenue.entity.enums;

/**
 * Loại thông báo trong hệ thống.
 * Ánh xạ CHECK constraint: ('Booking', 'Payment', 'Promotion', 'System') trong bảng notifications.
 */
public enum NotificationType {
    BOOKING,   // Thông báo liên quan đến đặt sân
    PAYMENT,   // Thông báo liên quan đến thanh toán / hoàn tiền
    PROMOTION, // Thông báo khuyến mãi
    SYSTEM     // Thông báo hệ thống
}
