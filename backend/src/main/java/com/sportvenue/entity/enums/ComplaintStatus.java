package com.sportvenue.entity.enums;

/**
 * Trạng thái khiếu nại của khách hàng.
 * Ánh xạ CHECK constraint trong bảng complaints (Migration V5).
 */
public enum ComplaintStatus {
    OPEN,              // Khiếu nại mới, chưa xử lý
    IN_PROGRESS,       // Đang được xử lý bởi Owner
    RESOLVED,          // Đã giải quyết xong (Owner tự resolve)
    ESCALATED,         // Đã được chuyển lên Admin xử lý
    PENDING_ADMIN_REVIEW, // Chờ KHÁCH HÀNG phản hồi trong 48h
    CUSTOMER_WITHDRAWN // Khách hàng tự rút khiếu nại
}
