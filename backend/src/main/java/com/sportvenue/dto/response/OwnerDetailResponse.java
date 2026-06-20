package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.ApprovedStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerDetailResponse {
    private Integer ownerId;
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String businessName;
    private String taxCode;
    private String businessAddress;
    private ApprovedStatus approvedStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
