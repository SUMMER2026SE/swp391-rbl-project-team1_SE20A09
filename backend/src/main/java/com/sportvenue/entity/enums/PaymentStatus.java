package com.sportvenue.entity.enums;

/**
 * Trạng thái thanh toán của đơn đặt sân.
 * Ánh xạ CHECK constraint: ('Unpaid', 'Paid', 'Refunded') trong bảng bookings.
 */
public enum PaymentStatus {
    UNPAID,   // Chưa thanh toán
    PAID,     // Đã thanh toán
    REFUNDED  // Đã hoàn tiền
}
