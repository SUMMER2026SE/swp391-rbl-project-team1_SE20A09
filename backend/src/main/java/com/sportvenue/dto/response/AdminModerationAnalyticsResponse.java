package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminModerationAnalyticsResponse {

    private Summary summary;
    private List<TopUser> topUsers;
    private List<Breakdown> categoryBreakdown;
    private List<Breakdown> statusBreakdown;
    private List<TrendPoint> trend;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private long totalSignals;
        private long totalReports;
        private long totalComplaints;
        private long openSignals;
        private long resolvedSignals;
        private double resolveRate;
        private double averageResolutionHours;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopUser {
        private Integer userId;
        private String fullName;
        private String email;
        private String roleName;
        private long reportCount;
        private long complaintCount;
        private long totalCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Breakdown {
        private String source;
        private String key;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendPoint {
        private LocalDate date;
        private long reportCount;
        private long complaintCount;
        private long totalCount;
    }
}
