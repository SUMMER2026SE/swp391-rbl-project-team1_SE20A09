package com.sportvenue.entity.enums;

/**
 * Trạng thái phê duyệt chủ sân từ Admin.
 * Ánh xạ CHECK constraint: ('PENDING', 'APPROVED', 'REJECTED') trong bảng owners.
 */
public enum ApprovedStatus {
    PENDING,    // Chờ Admin duyệt
    APPROVED,   // Đã được duyệt — có thể thêm/quản lý sân
    REJECTED    // Bị từ chối
}
