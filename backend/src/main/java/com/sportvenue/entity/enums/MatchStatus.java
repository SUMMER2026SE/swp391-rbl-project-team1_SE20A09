package com.sportvenue.entity.enums;

/**
 * Trạng thái của một kèo ghép.
 * Ánh xạ CHECK constraint: ('OPEN', 'FULL', 'CANCELLED', 'COMPLETED') trong bảng match_requests.
 */
public enum MatchStatus {
    OPEN,      // Kèo đang mở, cho phép đăng ký tham gia
    FULL,      // Kèo đã đủ người đăng ký
    CANCELLED, // Kèo đã bị hủy bởi chủ kèo
    COMPLETED  // Kèo đã hoàn thành (sau thời gian chơi)
}
