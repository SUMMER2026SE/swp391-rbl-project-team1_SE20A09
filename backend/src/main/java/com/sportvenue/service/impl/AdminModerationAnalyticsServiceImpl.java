package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.ReportRepository;
import com.sportvenue.service.AdminModerationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminModerationAnalyticsServiceImpl implements AdminModerationAnalyticsService {

    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final int MAX_TOP_LIMIT = 50;
    private static final String SOURCE_REPORT = "REPORT";
    private static final String SOURCE_COMPLAINT = "COMPLAINT";
    private static final String ROLE_CUSTOMER = "Customer";
    private static final String ROLE_OWNER = "Owner";

    private final ReportRepository reportRepository;
    private final ComplaintRepository complaintRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminModerationAnalyticsResponse getAnalytics(
            LocalDate from,
            LocalDate to,
            String role,
            String category,
            String status,
            int topLimit) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(29);
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("from must be before or equal to to.");
        }

        String normalizedRole = normalizeRole(role);
        FilterPair<ReportCategory, ComplaintPriority> categoryFilter = parseCategory(category);
        FilterPair<ReportStatus, ComplaintStatus> statusFilter = parseStatus(status);
        int limit = clampLimit(topLimit);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        SourceMetrics reportMetrics = loadReportMetrics(start, end, normalizedRole, categoryFilter, statusFilter);
        SourceMetrics complaintMetrics = loadComplaintMetrics(start, end, normalizedRole, categoryFilter, statusFilter);

        long totalSignals = reportMetrics.total + complaintMetrics.total;
        long openSignals = reportMetrics.open + complaintMetrics.open;
        long resolvedSignals = reportMetrics.resolved + complaintMetrics.resolved;

        return AdminModerationAnalyticsResponse.builder()
                .summary(AdminModerationAnalyticsResponse.Summary.builder()
                        .totalSignals(totalSignals)
                        .totalReports(reportMetrics.total)
                        .totalComplaints(complaintMetrics.total)
                        .openSignals(openSignals)
                        .resolvedSignals(resolvedSignals)
                        .resolveRate(totalSignals == 0 ? 0 : roundTwoDecimals((resolvedSignals * 100.0) / totalSignals))
                        .averageResolutionHours(averageResolutionHours(reportMetrics.durations, complaintMetrics.durations))
                        .build())
                .topUsers(mergeTopUsers(reportMetrics.topUsers, complaintMetrics.topUsers, limit))
                .categoryBreakdown(mergeBreakdowns(reportMetrics.categoryBreakdown, complaintMetrics.categoryBreakdown))
                .statusBreakdown(mergeBreakdowns(reportMetrics.statusBreakdown, complaintMetrics.statusBreakdown))
                .trend(mergeTrend(startDate, endDate, reportMetrics.trend, complaintMetrics.trend))
                .build();
    }

    private SourceMetrics loadReportMetrics(
            LocalDateTime start,
            LocalDateTime end,
            String role,
            FilterPair<ReportCategory, ComplaintPriority> categoryFilter,
            FilterPair<ReportStatus, ComplaintStatus> statusFilter) {
        if (!categoryFilter.includeReports || !statusFilter.includeReports) {
            return SourceMetrics.empty();
        }

        ReportCategory category = categoryFilter.reportValue;
        ReportStatus status = statusFilter.reportValue;
        long total = reportRepository.countModerationReports(start, end, role, status, category);
        List<AdminModerationAnalyticsResponse.Breakdown> statusBreakdown = toBreakdowns(
                SOURCE_REPORT,
                reportRepository.countModerationReportsByStatus(start, end, role, status, category));
        List<AdminModerationAnalyticsResponse.Breakdown> categoryBreakdown = toBreakdowns(
                SOURCE_REPORT,
                reportRepository.countModerationReportsByCategory(start, end, role, status, category));

        return SourceMetrics.builder()
                .total(total)
                .open(sumStatuses(statusBreakdown, ReportStatus.OPEN.name(), ReportStatus.UNDER_REVIEW.name()))
                .resolved(sumStatuses(statusBreakdown, ReportStatus.ACTION_TAKEN.name(), ReportStatus.DISMISSED.name()))
                .statusBreakdown(statusBreakdown)
                .categoryBreakdown(categoryBreakdown)
                .trend(toTrendMap(reportRepository.countModerationReportsByDate(start, end, role, status, category)))
                .topUsers(toTopUsers(
                        reportRepository.findTopReportedUsersForModeration(start, end, role, status, category),
                        true))
                .durations(toDurations(
                        reportRepository.findReportResolutionDurationsForModeration(start, end, role, status, category),
                        1))
                .build();
    }

    private SourceMetrics loadComplaintMetrics(
            LocalDateTime start,
            LocalDateTime end,
            String role,
            FilterPair<ReportCategory, ComplaintPriority> categoryFilter,
            FilterPair<ReportStatus, ComplaintStatus> statusFilter) {
        if (!categoryFilter.includeComplaints || !statusFilter.includeComplaints) {
            return SourceMetrics.empty();
        }

        ComplaintPriority priority = categoryFilter.complaintValue;
        ComplaintStatus status = statusFilter.complaintValue;
        long total = complaintRepository.countModerationComplaints(start, end, role, status, priority);
        List<AdminModerationAnalyticsResponse.Breakdown> statusBreakdown = toBreakdowns(
                SOURCE_COMPLAINT,
                complaintRepository.countModerationComplaintsByStatus(start, end, role, status, priority));
        List<AdminModerationAnalyticsResponse.Breakdown> categoryBreakdown = toBreakdowns(
                SOURCE_COMPLAINT,
                complaintRepository.countModerationComplaintsByPriority(start, end, role, status, priority));

        return SourceMetrics.builder()
                .total(total)
                .open(sumStatuses(
                        statusBreakdown,
                        ComplaintStatus.OPEN.name(),
                        ComplaintStatus.IN_PROGRESS.name(),
                        ComplaintStatus.ESCALATED.name(),
                        ComplaintStatus.PENDING_ADMIN_REVIEW.name()))
                .resolved(sumStatuses(
                        statusBreakdown,
                        ComplaintStatus.RESOLVED.name(),
                        ComplaintStatus.CUSTOMER_WITHDRAWN.name()))
                .statusBreakdown(statusBreakdown)
                .categoryBreakdown(categoryBreakdown)
                .trend(toTrendMap(complaintRepository.countModerationComplaintsByDate(start, end, role, status, priority)))
                .topUsers(toTopUsers(
                        complaintRepository.findTopComplainedUsersForModeration(start, end, role, status, priority),
                        false))
                .durations(toDurations(
                        complaintRepository.findComplaintResolutionDurationsForModeration(start, end, role, status, priority),
                        2))
                .build();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return null;
        }
        if (ROLE_CUSTOMER.equalsIgnoreCase(role)) {
            return ROLE_CUSTOMER;
        }
        if (ROLE_OWNER.equalsIgnoreCase(role)) {
            return ROLE_OWNER;
        }
        throw new BadRequestException("role must be Customer or Owner.");
    }

    private FilterPair<ReportCategory, ComplaintPriority> parseCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank() || "ALL".equalsIgnoreCase(rawCategory)) {
            return FilterPair.includeAll();
        }
        String value = rawCategory.trim().toUpperCase(Locale.ROOT);
        ReportCategory reportCategory = tryParseEnum(ReportCategory.class, value);
        ComplaintPriority complaintPriority = tryParseEnum(ComplaintPriority.class, value);
        if (reportCategory == null && complaintPriority == null) {
            throw new BadRequestException("Unknown moderation category.");
        }
        return FilterPair.<ReportCategory, ComplaintPriority>builder()
                .reportValue(reportCategory)
                .complaintValue(complaintPriority)
                .includeReports(reportCategory != null)
                .includeComplaints(complaintPriority != null)
                .build();
    }

    private FilterPair<ReportStatus, ComplaintStatus> parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "ALL".equalsIgnoreCase(rawStatus)) {
            return FilterPair.includeAll();
        }
        String value = rawStatus.trim().toUpperCase(Locale.ROOT);
        ReportStatus reportStatus = tryParseEnum(ReportStatus.class, value);
        ComplaintStatus complaintStatus = tryParseEnum(ComplaintStatus.class, value);
        if (reportStatus == null && complaintStatus == null) {
            throw new BadRequestException("Unknown moderation status.");
        }
        return FilterPair.<ReportStatus, ComplaintStatus>builder()
                .reportValue(reportStatus)
                .complaintValue(complaintStatus)
                .includeReports(reportStatus != null)
                .includeComplaints(complaintStatus != null)
                .build();
    }

    private <T extends Enum<T>> T tryParseEnum(Class<T> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int clampLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_TOP_LIMIT;
        }
        return Math.min(requestedLimit, MAX_TOP_LIMIT);
    }

    private List<AdminModerationAnalyticsResponse.Breakdown> toBreakdowns(String source, List<Object[]> rows) {
        return rows.stream()
                .map(row -> AdminModerationAnalyticsResponse.Breakdown.builder()
                        .source(source)
                        .key(String.valueOf(row[0]))
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    private long sumStatuses(List<AdminModerationAnalyticsResponse.Breakdown> breakdown, String... statuses) {
        List<String> keys = List.of(statuses);
        return breakdown.stream()
                .filter(item -> keys.contains(item.getKey()))
                .mapToLong(AdminModerationAnalyticsResponse.Breakdown::getCount)
                .sum();
    }

    private Map<LocalDate, Long> toTrendMap(List<Object[]> rows) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(toLocalDate(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private List<AdminModerationAnalyticsResponse.TopUser> toTopUsers(List<Object[]> rows, boolean reportSource) {
        return rows.stream()
                .map(row -> {
                    String fullName = ((String) row[1] + " " + (String) row[2]).trim();
                    long count = ((Number) row[5]).longValue();
                    return AdminModerationAnalyticsResponse.TopUser.builder()
                            .userId((Integer) row[0])
                            .fullName(fullName)
                            .email((String) row[3])
                            .roleName((String) row[4])
                            .reportCount(reportSource ? count : 0)
                            .complaintCount(reportSource ? 0 : count)
                            .totalCount(count)
                            .build();
                })
                .toList();
    }

    private List<AdminModerationAnalyticsResponse.TopUser> mergeTopUsers(
            List<AdminModerationAnalyticsResponse.TopUser> reportUsers,
            List<AdminModerationAnalyticsResponse.TopUser> complaintUsers,
            int limit) {
        Map<Integer, AdminModerationAnalyticsResponse.TopUser> merged = new LinkedHashMap<>();
        for (AdminModerationAnalyticsResponse.TopUser user : reportUsers) {
            merged.put(user.getUserId(), user);
        }
        for (AdminModerationAnalyticsResponse.TopUser user : complaintUsers) {
            AdminModerationAnalyticsResponse.TopUser existing = merged.get(user.getUserId());
            if (existing == null) {
                merged.put(user.getUserId(), user);
            } else {
                existing.setComplaintCount(user.getComplaintCount());
                existing.setTotalCount(existing.getReportCount() + existing.getComplaintCount());
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparingLong(AdminModerationAnalyticsResponse.TopUser::getTotalCount).reversed())
                .limit(limit)
                .toList();
    }

    private List<AdminModerationAnalyticsResponse.Breakdown> mergeBreakdowns(
            List<AdminModerationAnalyticsResponse.Breakdown> reports,
            List<AdminModerationAnalyticsResponse.Breakdown> complaints) {
        List<AdminModerationAnalyticsResponse.Breakdown> result = new ArrayList<>();
        result.addAll(reports);
        result.addAll(complaints);
        return result;
    }

    private List<AdminModerationAnalyticsResponse.TrendPoint> mergeTrend(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, Long> reportTrend,
            Map<LocalDate, Long> complaintTrend) {
        List<AdminModerationAnalyticsResponse.TrendPoint> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            long reportCount = reportTrend.getOrDefault(current, 0L);
            long complaintCount = complaintTrend.getOrDefault(current, 0L);
            result.add(AdminModerationAnalyticsResponse.TrendPoint.builder()
                    .date(current)
                    .reportCount(reportCount)
                    .complaintCount(complaintCount)
                    .totalCount(reportCount + complaintCount)
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }

    private List<Duration> toDurations(List<Object[]> rows, int resolvedColumnIndex) {
        return rows.stream()
                .map(row -> {
                    LocalDateTime createdAt = (LocalDateTime) row[0];
                    LocalDateTime resolvedAt = (LocalDateTime) row[resolvedColumnIndex];
                    if (resolvedAt == null && row.length > 1) {
                        resolvedAt = (LocalDateTime) row[1];
                    }
                    return resolvedAt == null || resolvedAt.isBefore(createdAt)
                            ? null
                            : Duration.between(createdAt, resolvedAt);
                })
                .filter(duration -> duration != null)
                .toList();
    }

    private double averageResolutionHours(List<Duration> reportDurations, List<Duration> complaintDurations) {
        List<Duration> allDurations = new ArrayList<>();
        allDurations.addAll(reportDurations);
        allDurations.addAll(complaintDurations);
        if (allDurations.isEmpty()) {
            return 0;
        }
        double average = allDurations.stream()
                .mapToDouble(duration -> duration.toMinutes() / 60.0)
                .average()
                .orElse(0);
        return roundTwoDecimals(average);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @lombok.Builder
    private record FilterPair<L, R>(
            L reportValue,
            R complaintValue,
            boolean includeReports,
            boolean includeComplaints) {

        private static <L, R> FilterPair<L, R> includeAll() {
            return new FilterPair<>(null, null, true, true);
        }
    }

    @lombok.Builder
    private record SourceMetrics(
            long total,
            long open,
            long resolved,
            List<AdminModerationAnalyticsResponse.TopUser> topUsers,
            List<AdminModerationAnalyticsResponse.Breakdown> categoryBreakdown,
            List<AdminModerationAnalyticsResponse.Breakdown> statusBreakdown,
            Map<LocalDate, Long> trend,
            List<Duration> durations) {

        private static SourceMetrics empty() {
            return new SourceMetrics(0, 0, 0, List.of(), List.of(), List.of(), Map.of(), List.of());
        }
    }
}
