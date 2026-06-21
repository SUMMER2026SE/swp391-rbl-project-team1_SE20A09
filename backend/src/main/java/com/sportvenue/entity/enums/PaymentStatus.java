package com.sportvenue.entity.enums;

/**
 * Trạng thái thanh toán của đơn đặt sân.
 * Ánh xạ CHECK constraint {@code bookings_payment_status_check} trong DB:
 * ('UNPAID', 'PAID', 'REFUND_PENDING', 'REFUNDED', 'DEPOSITED', 'FAILED').
 */
public enum PaymentStatus {
    UNPAID,         // Chưa thanh toán
    PAID,           // Đã thanh toán
    REFUND_PENDING, // Đã huỷ, đang chờ chủ sân duyệt hoàn tiền (UC-CUS-06)
    REFUNDED,       // Đã hoàn tiền
    DEPOSITED,      // Đã đặt cọc (VNPay deposit flow)
    FAILED          // Thanh toán thất bại
}