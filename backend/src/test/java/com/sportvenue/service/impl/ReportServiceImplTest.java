package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReportRequest;
import com.sportvenue.dto.request.ResolveReportRequest;
import com.sportvenue.dto.response.ReportResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Report;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.ReportRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MatchRequestRepository matchRequestRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void createReport_ownerReportsCustomerThroughBooking_success() {
        User customer = user(10, "customer@example.com", "Customer");
        User ownerUser = user(20, "owner@example.com", "Owner");
        Booking booking = booking(customer, ownerUser);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(reportRepository.countByReporterUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setReportId(99);
            return report;
        });

        ReportResponse response = reportService.createReport(CreateReportRequest.builder()
                .reporteeId(10)
                .bookingId(1)
                .category(ReportCategory.PROPERTY_DAMAGE)
                .description("Làm hỏng mặt sân")
                .evidenceUrls(List.of("https://example.com/a.png"))
                .build(), "owner@example.com");

        assertEquals(99, response.getReportId());
        assertEquals(ReportCategory.PROPERTY_DAMAGE, response.getCategory());
        assertEquals(20, response.getReporter().getUserId());
        assertEquals(10, response.getReportee().getUserId());
        assertEquals(List.of("https://example.com/a.png"), response.getEvidenceUrls());
    }

    @Test
    void createReport_unrelatedReporter_throwsBadRequest() {
        User customer = user(10, "customer@example.com", "Customer");
        User ownerUser = user(20, "owner@example.com", "Owner");
        User unrelated = user(30, "other@example.com", "Customer");
        Booking booking = booking(customer, ownerUser);

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(unrelated));
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(reportRepository.countByReporterUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> reportService.createReport(CreateReportRequest.builder()
                .reporteeId(10)
                .bookingId(1)
                .category(ReportCategory.HARASSMENT)
                .description("Không liên quan")
                .build(), "other@example.com"));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_customerReportsOwnerFakeListingThroughStadium_success() {
        User customer = user(10, "customer@example.com", "Customer");
        User ownerUser = user(20, "owner@example.com", "Owner");
        Stadium stadium = stadium(ownerUser);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(userRepository.findById(20)).thenReturn(Optional.of(ownerUser));
        when(reportRepository.countByReporterUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(stadiumRepository.findById(100)).thenReturn(Optional.of(stadium));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setReportId(1000);
            return report;
        });

        ReportResponse response = reportService.createReport(CreateReportRequest.builder()
                .reporteeId(20)
                .stadiumId(100)
                .category(ReportCategory.FAKE_LISTING)
                .description("Fake listing")
                .build(), "customer@example.com");

        assertEquals(1000, response.getReportId());
        assertEquals(ReportCategory.FAKE_LISTING, response.getCategory());
        assertEquals(10, response.getReporter().getUserId());
        assertEquals(20, response.getReportee().getUserId());
        assertEquals(100, response.getStadiumId());
    }

    @Test
    void createReport_stadiumOnlyQualityIssue_throwsBadRequest() {
        User customer = user(10, "customer@example.com", "Customer");
        User ownerUser = user(20, "owner@example.com", "Owner");
        Stadium stadium = stadium(ownerUser);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(userRepository.findById(20)).thenReturn(Optional.of(ownerUser));
        when(reportRepository.countByReporterUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(stadiumRepository.findById(100)).thenReturn(Optional.of(stadium));

        assertThrows(BadRequestException.class, () -> reportService.createReport(CreateReportRequest.builder()
                .reporteeId(20)
                .stadiumId(100)
                .category(ReportCategory.OTHER)
                .description("Service quality issue")
                .build(), "customer@example.com"));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_dailyLimitReached_throwsBadRequest() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(10)).thenReturn(Optional.of(reportee));
        when(reportRepository.countByReporterUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(5L);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reportService.createReport(CreateReportRequest.builder()
                        .reporteeId(10)
                        .bookingId(1)
                        .category(ReportCategory.NO_SHOW)
                        .description("Không đến sân")
                        .build(), "owner@example.com"));

        assertTrue(exception.getMessage().contains("giới hạn"));
        verify(bookingRepository, never()).findById(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void getMyReport_reporteeCannotView_throwsBadRequest() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        Report report = Report.builder().reportId(99).reporter(reporter).reportee(reportee).build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(reportee));
        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));

        assertThrows(BadRequestException.class, () -> reportService.getMyReport(99, "customer@example.com"));
    }

    @Test
    void updateReportStatus_ActionTakenFirstVerifiedReport_NotifiesReporterAndWarnsReportee() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.OPEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.ACTION_TAKEN);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.countByReporteeUserIdAndCategoryAndStatus(
                10, ReportCategory.HARASSMENT, ReportStatus.ACTION_TAKEN)).thenReturn(1L);

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(notificationService).createNotification(
                eq(20),
                eq("Báo cáo đã được xử lý"),
                any(),
                eq(NotificationType.REPORT),
                eq("REPORT-99"));
        verify(notificationService).createNotification(
                eq(10),
                eq("Cảnh báo hành vi"),
                any(),
                eq(NotificationType.REPORT),
                eq("REPORT-99"));
        verify(userRepository, never()).findAllAdmins();
    }

    @Test
    void updateReportStatus_ActionTakenSecondVerifiedReport_DoesNotWarnOrFlagAgain() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.OPEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.ACTION_TAKEN);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.countByReporteeUserIdAndCategoryAndStatus(
                10, ReportCategory.HARASSMENT, ReportStatus.ACTION_TAKEN)).thenReturn(2L);

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(notificationService, never()).createNotification(
                eq(10),
                eq("Cảnh báo hành vi"),
                any(),
                eq(NotificationType.REPORT),
                any());
        verify(userRepository, never()).findAllAdmins();
    }

    @Test
    void updateReportStatus_ActionTakenThirdVerifiedReport_FlagsAdminsForManualReview() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        User reviewer = user(2, "reviewer@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.OPEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.ACTION_TAKEN);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.countByReporteeUserIdAndCategoryAndStatus(
                10, ReportCategory.HARASSMENT, ReportStatus.ACTION_TAKEN)).thenReturn(3L);
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin, reviewer));

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(notificationService).createNotification(
                eq(1),
                eq("User cần review khẩn"),
                any(),
                eq(NotificationType.REPORT),
                eq("USER-10-REPORT-HARASSMENT"));
        verify(notificationService).createNotification(
                eq(2),
                eq("User cần review khẩn"),
                any(),
                eq(NotificationType.REPORT),
                eq("USER-10-REPORT-HARASSMENT"));
    }

    @Test
    void updateReportStatus_ActionTakenFourthVerifiedReport_DoesNotRepeatAdminFlag() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.OPEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.ACTION_TAKEN);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.countByReporteeUserIdAndCategoryAndStatus(
                10, ReportCategory.HARASSMENT, ReportStatus.ACTION_TAKEN)).thenReturn(4L);

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(userRepository, never()).findAllAdmins();
    }

    @Test
    void updateReportStatus_SameFinalStatus_DoesNotNotifyReporterOrRecountStrike() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.ACTION_TAKEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.ACTION_TAKEN);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(reportRepository, never()).countByReporteeUserIdAndCategoryAndStatus(any(), any(), any());
    }

    @Test
    void updateReportStatus_UnderReview_DoesNotNotifyReporterOrHandleStrike() {
        User reporter = user(20, "owner@example.com", "Owner");
        User reportee = user(10, "customer@example.com", "Customer");
        User admin = user(1, "admin@example.com", "Admin");
        Report report = report(99, reporter, reportee, ReportStatus.OPEN, ReportCategory.HARASSMENT);
        ResolveReportRequest request = resolveRequest(ReportStatus.UNDER_REVIEW);

        when(reportRepository.findWithDetailsByReportId(99)).thenReturn(Optional.of(report));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportService.updateReportStatus(99, request, "admin@example.com");

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(reportRepository, never()).countByReporteeUserIdAndCategoryAndStatus(any(), any(), any());
    }

    private User user(Integer id, String email, String roleName) {
        Role role = Role.builder().roleName(roleName).build();
        return User.builder()
                .userId(id)
                .firstName("Test")
                .lastName(roleName)
                .email(email)
                .role(role)
                .build();
    }

    private Report report(
            Integer id,
            User reporter,
            User reportee,
            ReportStatus status,
            ReportCategory category) {
        return Report.builder()
                .reportId(id)
                .reporter(reporter)
                .reportee(reportee)
                .status(status)
                .category(category)
                .description("Report description")
                .build();
    }

    private ResolveReportRequest resolveRequest(ReportStatus status) {
        ResolveReportRequest request = new ResolveReportRequest();
        request.setStatus(status);
        request.setResolutionNote("Reviewed");
        return request;
    }

    private Booking booking(User customer, User ownerUser) {
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        Stadium stadium = Stadium.builder().stadiumId(100).stadiumName("Sân A").owner(owner).build();
        return Booking.builder().bookingId(1).user(customer).stadium(stadium).build();
    }

    private Stadium stadium(User ownerUser) {
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        return Stadium.builder().stadiumId(100).stadiumName("San A").owner(owner).build();
    }
}
