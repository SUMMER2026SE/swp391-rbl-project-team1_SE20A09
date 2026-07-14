package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.ReportStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminModerationAnalyticsServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private AdminModerationAnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        stubEmptyReportMetrics();
        stubEmptyComplaintMetrics();
    }

    @Test
    void getAnalytics_AppliesReportAndComplaintStatusFiltersIndependently() {
        analyticsService.getAnalytics(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                null,
                null,
                null,
                ReportStatus.OPEN,
                ComplaintStatus.OPEN,
                10);

        verify(reportRepository).countModerationReports(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                eq(ReportStatus.OPEN),
                isNull());
        verify(complaintRepository).countModerationComplaints(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                eq(ComplaintStatus.OPEN),
                isNull());
    }

    @Test
    void getAnalytics_ComplaintPriorityFilter_DoesNotHideReportSignals() {
        // Regression test: complaintPriority and reportCategory used to share 1 string param —
        // picking a Complaint-only value (e.g. HIGH) silently zeroed out Report entirely because
        // it couldn't be parsed as a ReportCategory. Now they're independent params, so a
        // complaint-only filter must not skip the report queries at all.
        analyticsService.getAnalytics(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                null,
                null,
                ComplaintPriority.HIGH,
                null,
                null,
                10);

        verify(reportRepository).countModerationReports(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                isNull(),
                isNull());
        verify(complaintRepository).countModerationComplaints(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                isNull(),
                eq(ComplaintPriority.HIGH));
    }

    @Test
    void getAnalytics_MergesTopUsersByUserIdAndSortsByTotalCount() {
        when(reportRepository.findTopReportedUsersForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(
                        topUserRow(10, "Alex", "Nguyen", "alex@example.com", "Customer", 2)));
        when(complaintRepository.findTopComplainedUsersForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(
                        topUserRow(10, "Alex", "Nguyen", "alex@example.com", "Customer", 3),
                        topUserRow(20, "Bao", "Tran", "bao@example.com", "Owner", 4)));

        AdminModerationAnalyticsResponse response = analyticsService.getAnalytics(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                null,
                null,
                null,
                null,
                null,
                10);

        assertEquals(2, response.getTopUsers().size());
        AdminModerationAnalyticsResponse.TopUser first = response.getTopUsers().get(0);
        assertEquals(10, first.getUserId());
        assertEquals(2, first.getReportCount());
        assertEquals(3, first.getComplaintCount());
        assertEquals(5, first.getTotalCount());
        assertEquals(20, response.getTopUsers().get(1).getUserId());
    }

    @Test
    void getAnalytics_RejectsTrendRangesLongerThanOneYear() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                analyticsService.getAnalytics(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 1, 2),
                        null,
                        null,
                        null,
                        null,
                        null,
                        10));

        assertEquals("Date range must not exceed 366 days.", exception.getMessage());
        verifyNoInteractions(reportRepository);
        verifyNoInteractions(complaintRepository);
    }

    @Test
    void getAnalytics_BuildsTrendForEveryDayInRange() {
        when(reportRepository.countModerationReportsByDate(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {java.sql.Date.valueOf("2026-07-01"), 2L}));
        when(complaintRepository.countModerationComplaintsByDate(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {java.sql.Date.valueOf("2026-07-02"), 3L}));

        AdminModerationAnalyticsResponse response = analyticsService.getAnalytics(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 3),
                null,
                null,
                null,
                null,
                null,
                10);

        assertEquals(3, response.getTrend().size());
        assertEquals(2, response.getTrend().get(0).getReportCount());
        assertEquals(0, response.getTrend().get(0).getComplaintCount());
        assertEquals(0, response.getTrend().get(1).getReportCount());
        assertEquals(3, response.getTrend().get(1).getComplaintCount());
        assertEquals(0, response.getTrend().get(2).getTotalCount());
    }

    private void stubEmptyReportMetrics() {
        when(reportRepository.countModerationReports(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(0L);
        when(reportRepository.countModerationReportsByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.countModerationReportsByCategory(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.countModerationReportsByDate(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.findTopReportedUsersForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.findReportResolutionDurationsForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
    }

    private void stubEmptyComplaintMetrics() {
        when(complaintRepository.countModerationComplaints(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(0L);
        when(complaintRepository.countModerationComplaintsByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(complaintRepository.countModerationComplaintsByPriority(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(complaintRepository.countModerationComplaintsByDate(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(complaintRepository.findTopComplainedUsersForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
        when(complaintRepository.findComplaintResolutionDurationsForModeration(
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), any()))
                .thenReturn(List.of());
    }

    private Object[] topUserRow(
            Integer userId,
            String firstName,
            String lastName,
            String email,
            String roleName,
            long count) {
        return new Object[] {userId, firstName, lastName, email, roleName, count};
    }
}
