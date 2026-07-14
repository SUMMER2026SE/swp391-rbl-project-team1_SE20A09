package com.sportvenue.dto.response;

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
    private String lockReason;
    /** Chỉ có giá trị khi roleName = "Owner" — null với Customer/Admin. Dùng để
     * frontend gate UI Owner (menu/dashboard) theo đúng trạng thái duyệt hồ sơ,
     * không chỉ dựa vào role (xem docs/auth_account_flow_findings.md #2). */
    private String ownerApprovedStatus;
}
