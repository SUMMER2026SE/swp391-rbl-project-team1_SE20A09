package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOwnerResponse {
    private Integer ownerId;
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    
    private String businessName;
    private String taxCode;
    private String businessAddress;
    
    private String approvedStatus;
    private String accountStatus;
    private LocalDateTime createdAt;
}
