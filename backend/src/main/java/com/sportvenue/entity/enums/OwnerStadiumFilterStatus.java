package com.sportvenue.entity.enums;

/**
 * Các giá trị lọc sân được phép truyền vào API GET /api/v1/stadiums/my.
 * Chỉ 3 giá trị logic: ACTIVE (đang hoạt động), PENDING (chờ duyệt),
 * SUSPENDED (tạm dừng). Không dùng giá trị StadiumStatus thô để tránh
 * lộ implementation detail và tránh filter "chết" (CLOSED bị loại từ query).
 */
public enum OwnerStadiumFilterStatus {
    ACTIVE,
    PENDING,
    SUSPENDED
}
