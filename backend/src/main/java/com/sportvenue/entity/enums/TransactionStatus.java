package com.sportvenue.entity.enums;

/**
 * Trạng thái giao dịch thanh toán.
 * Ánh xạ CHECK constraint: ('PENDING', 'SUCCESS', 'FAILED') trong bảng payments.
 */
public enum TransactionStatus {
    PENDING, // Đang chờ xử lý
    SUCCESS, // Giao dịch thành công
    FAILED   // Giao dịch thất bại
}
