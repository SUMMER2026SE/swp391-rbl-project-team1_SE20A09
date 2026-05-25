package com.sportvenue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Integer userId;
    private String email;
    private String firstName;
    private String lastName;
    private String roleName;
    private String avatarUrl;
    private String phoneNumber;
    private String userRank;
    private Integer userPoint;
    private String accountStatus;
}
