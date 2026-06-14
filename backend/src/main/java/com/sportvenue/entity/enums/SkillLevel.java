package com.sportvenue.entity.enums;

/**
 * Trình độ yêu cầu của người chơi đối với kèo ghép.
 * Ánh xạ CHECK constraint: ('BEGINNER', 'INTERMEDIATE', 'ADVANCED') trong bảng match_requests.
 */
public enum SkillLevel {
    BEGINNER,     // Người mới bắt đầu
    INTERMEDIATE, // Trình độ trung bình
    ADVANCED      // Trình độ nâng cao/chuyên nghiệp
}
