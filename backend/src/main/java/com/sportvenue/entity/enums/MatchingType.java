package com.sportvenue.entity.enums;

/**
 * Phân loại hình thức ghép kèo: cá nhân ghép lẻ vs đội tìm đối thủ.
 * Ánh xạ CHECK constraint: ('INDIVIDUAL', 'TEAM_VS_TEAM') trong bảng match_requests.
 */
public enum MatchingType {
    INDIVIDUAL,   // Ghép lẻ (Tìm người chơi lẻ để ghép đủ đội)
    TEAM_VS_TEAM  // Ghép đội (Đội tìm đối thủ cáp kèo đá giao lưu)
}
