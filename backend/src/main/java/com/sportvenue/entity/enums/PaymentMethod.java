package com.sportvenue.entity.enums;

/**
 * Phương thức thanh toán.
 * Ánh xạ CHECK constraint: ('Cash', 'VNPay', 'Momo', 'Banking') trong bảng payments.
 */
public enum PaymentMethod {
    CASH,    // Thanh toán tiền mặt tại sân
    VNPAY,   // Cổng thanh toán VNPay
    MOMO,    // Ví điện tử MoMo
    BANKING  // Chuyển khoản ngân hàng
}
