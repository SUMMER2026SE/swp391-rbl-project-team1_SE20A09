package com.sportvenue.entity.enums;

/**
 * Trạng thái thanh toán của đơn đặt sân.
 * Ánh xạ CHECK constraint: ('UNPAID', 'PAID', 'REFUNDED', 'DEPOSITED', 'AWAITING_CASH_PAYMENT')
 * trong bảng bookings (xem migration V5.8, V102).
 */
public enum PaymentStatus {
    UNPAID,   // Chưa thanh toán
    PAID,     // Đã thanh toán thật qua cổng thanh toán (VNPay/MoMo)
    REFUNDED, // Đã hoàn tiền
    DEPOSITED, // Đã đặt cọc
    AWAITING_CASH_PAYMENT // Khách xác nhận sẽ trả tiền mặt tại sân — CHƯA có gateway/ai xác nhận đã thu tiền thật
}
