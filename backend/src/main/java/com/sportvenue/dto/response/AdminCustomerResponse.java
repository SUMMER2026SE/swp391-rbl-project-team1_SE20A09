package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.UserRank;

/**
 * DTO trả về thông tin khách hàng cho Admin — UC-ADM-02.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCustomerResponse {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private AccountStatus accountStatus;
    private UserRank userRank;
    private Integer userPoint;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
