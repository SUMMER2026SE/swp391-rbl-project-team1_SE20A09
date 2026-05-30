package com.sportvenue.entity.enums;

/**
 * Trạng thái hoạt động của sân thể thao.
 * Ánh xạ CHECK constraint: ('Available', 'Maintenance', 'Closed') trong bảng stadiums.
 */
public enum StadiumStatus {
    AVAILABLE,    // Sân đang hoạt động, có thể đặt
    MAINTENANCE,  // Đang bảo trì — tạm ngưng nhận đặt sân
    CLOSED        // Đã đóng cửa vĩnh viễn / bị xóa
}
