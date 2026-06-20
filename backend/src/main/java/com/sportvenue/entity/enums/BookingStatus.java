package com.sportvenue.entity.enums;

/**
 * Trạng thái đơn đặt sân.
 * Ánh xạ CHECK constraint (cập nhật V5.7): ('PENDING_PAYMENT', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED').
 */
public enum BookingStatus {
    /** UC-CUS-01: Booking vừa tạo — chờ khách thanh toán. Slot được giữ đến {@code expiredAt} (5 phút). */
    PENDING_PAYMENT,
    /** Chờ Owner xác nhận (legacy flow — khi không cần payment trước). */
    PENDING,
    /** Đã thanh toán / Owner đã xác nhận. */
    CONFIRMED,
    /** Đã hoàn thành (sau giờ chơi). */
    COMPLETED,
    /** Đã hủy (do khách hủy, owner từ chối, hoặc scheduler quá hạn thanh toán). */
    CANCELLED
}
