package com.sportvenue.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
    Integer userId,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phoneNumber,
    String avatarUrl,
    String roleName,
    Integer userPoint,
    String userRank,
    String accountStatus,
    LocalDateTime createdAt
) {
}

