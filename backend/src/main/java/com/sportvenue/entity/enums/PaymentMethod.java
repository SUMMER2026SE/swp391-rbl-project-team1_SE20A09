package com.sportvenue.entity.enums;

/**
 * Phương thức thanh toán.
 * Ánh xạ CHECK constraint: ('CASH', 'VNPAY', 'MOMO', 'BANKING', 'WALLET') trong bảng payments.
 */
public enum PaymentMethod {
    CASH,    // Thanh toán tiền mặt tại sân
    VNPAY,   // Cổng thanh toán VNPay
    MOMO,    // Ví điện tử MoMo
    BANKING, // Chuyển khoản ngân hàng
    WALLET   // Ví nội bộ Customer
}
