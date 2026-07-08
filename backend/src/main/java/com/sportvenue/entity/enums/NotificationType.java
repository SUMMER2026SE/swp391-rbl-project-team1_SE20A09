package com.sportvenue.entity.enums;

/**
 * Loại thông báo trong hệ thống.
 */
public enum NotificationType {
    BOOKING,        // Thông báo liên quan đến đặt sân
    PAYMENT,        // Thông báo liên quan đến thanh toán / hoàn tiền
    PROMOTION,      // Thông báo khuyến mãi
    SYSTEM,         // Thông báo hệ thống
    REVIEW,         // Thông báo đánh giá
    COMPLAINT,      // Thông báo khiếu nại
    OWNER_APPROVAL, // Thông báo chủ sân mới đăng ký chờ duyệt (Admin)
    STADIUM_APPROVAL // Thông báo sân mới chờ duyệt (Admin)
}
