package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.AppealStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AppealResponse {
    private Integer appealId;
    private Integer userId;
    private String userEmail;
    private String userFullName;
    private String relatedLockReason;
    private String appealText;
    private List<String> evidenceUrls;
    private AppealStatus status;
    private Integer reviewedByAdminId;
    private String reviewedByAdminEmail;
    private LocalDateTime reviewedAt;
    private String adminNote;
    private LocalDateTime createdAt;
}
