package com.sportvenue.entity.enums;

/**
 * Trạng thái khiếu nại của khách hàng.
 * Ánh xạ CHECK constraint trong bảng complaints (Migration V5).
 */
public enum ComplaintStatus {
    OPEN,        // Khiếu nại mới, chưa xử lý
    IN_PROGRESS, // Đang được xử lý bởi Owner
    RESOLVED     // Đã giải quyết xong
}
