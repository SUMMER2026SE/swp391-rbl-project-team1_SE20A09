package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Integer reportId;
    private UserSummary reporter;
    private UserSummary reportee;
    private Integer bookingId;
    private Integer matchRequestId;
    private Integer joinRequestId;
    private Integer stadiumId;
    private String stadiumName;
    private ReportCategory category;
    private String description;
    private List<String> evidenceUrls;
    private ReportStatus status;
    private UserSummary resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Integer userId;
        private String fullName;
        private String email;
        private String roleName;
    }
}
