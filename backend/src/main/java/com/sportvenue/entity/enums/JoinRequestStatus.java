package com.sportvenue.entity.enums;

/**
 * Trạng thái yêu cầu xin gia nhập một kèo ghép.
 * Ánh xạ CHECK constraint: ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') trong bảng join_requests.
 */
public enum JoinRequestStatus {
    PENDING,   // Đang chờ chủ kèo phê duyệt
    APPROVED,  // Đã được chấp nhận tham gia
    REJECTED,  // Yêu cầu bị chủ kèo từ chối
    CANCELLED  // Người xin gia nhập tự hủy yêu cầu
}
