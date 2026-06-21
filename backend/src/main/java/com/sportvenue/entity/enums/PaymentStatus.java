package com.sportvenue.entity.enums;

/**
 * Trạng thái thanh toán của đơn đặt sân.
 * Ánh xạ CHECK constraint: ('UNPAID', 'PAID', 'REFUNDED') trong bảng bookings.
 */
public enum PaymentStatus {
    UNPAID,   // Chưa thanh toán
    PAID,     // Đã thanh toán
    REFUNDED, // Đã hoàn tiền
    DEPOSITED // Đã đặt cọc
}
