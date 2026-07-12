package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReportRequest;
import com.sportvenue.dto.request.ResolveReportRequest;
import com.sportvenue.dto.response.ReportResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Report;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.ReportRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final int DAILY_REPORT_LIMIT = 5;
    private static final int VERIFIED_REPORT_REVIEW_THRESHOLD = 3;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final StadiumRepository stadiumRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReportResponse createReport(CreateReportRequest request, String reporterEmail) {
        User reporter = findUserByEmail(reporterEmail);
        User reportee = userRepository.findById(request.getReporteeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người bị báo cáo."));

        validateReporter(reporter, reportee);
        enforceDailyLimit(reporter);

        ReportContext context = resolveContext(request);
        validateRealInteraction(reporter, reportee, context);

        Report report = Report.builder()
                .reporter(reporter)
                .reportee(reportee)
                .booking(context.booking)
                .matchRequest(context.matchRequest)
                .joinRequest(context.joinRequest)
                .stadium(context.stadium)
                .category(request.getCategory())
                .description(request.getDescription().trim())
                .evidenceUrls(cleanEvidenceUrls(request.getEvidenceUrls()))
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        Report saved = reportRepository.save(report);
        notifyReporteeAboutNewReport(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(String reporterEmail, Pageable pageable) {
        User reporter = findUserByEmail(reporterEmail);
        return reportRepository.findByReporterUserIdOrderByCreatedAtDesc(reporter.getUserId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getMyReport(Integer reportId, String reporterEmail) {
        User reporter = findUserByEmail(reporterEmail);
        Report report = findReport(reportId);
        if (!report.getReporter().getUserId().equals(reporter.getUserId())) {
            throw new BadRequestException("Bạn không có quyền xem báo cáo này.");
        }
        return mapToResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAdminReports(ReportStatus status, ReportCategory category, Pageable pageable) {
        return reportRepository.findForAdmin(status, category, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getAdminReport(Integer reportId) {
        return mapToResponse(findReport(reportId));
    }

    @Override
    @Transactional
    public ReportResponse updateReportStatus(Integer reportId, ResolveReportRequest request, String adminEmail) {
        Report report = findReport(reportId);
        User admin = findUserByEmail(adminEmail);
        ReportStatus previousStatus = report.getStatus();
        ReportStatus status = request.getStatus();

        report.setStatus(status);
        report.setResolutionNote(cleanText(request.getResolutionNote()));
        if (status == ReportStatus.ACTION_TAKEN || status == ReportStatus.DISMISSED) {
            report.setResolvedBy(admin);
            report.setResolvedAt(LocalDateTime.now());
        } else {
            report.setResolvedBy(null);
            report.setResolvedAt(null);
        }

        Report saved = reportRepository.save(report);
        notifyReporterAboutResolution(saved, previousStatus);
        handleVerifiedReportStrike(saved, previousStatus);
        return mapToResponse(saved);
    }

    private void notifyReporteeAboutNewReport(Report report) {
        notificationService.createNotification(
                report.getReportee().getUserId(),
                "Bạn có một báo cáo mới",
                "Một báo cáo hành vi đã được gửi tới Admin để xem xét. Hệ thống không hiển thị danh tính người báo cáo.",
                NotificationType.REPORT,
                "REPORT-" + report.getReportId());
    }

    private void notifyReporterAboutResolution(Report report, ReportStatus previousStatus) {
        if (previousStatus == report.getStatus()) {
            return;
        }
        if (report.getStatus() != ReportStatus.ACTION_TAKEN && report.getStatus() != ReportStatus.DISMISSED) {
            return;
        }
        notificationService.createNotification(
                report.getReporter().getUserId(),
                "Báo cáo đã được xử lý",
                report.getStatus() == ReportStatus.ACTION_TAKEN
                        ? "Admin đã xác nhận báo cáo và thực hiện hành động phù hợp."
                        : "Admin đã xem xét và bác bỏ báo cáo này.",
                NotificationType.REPORT,
                "REPORT-" + report.getReportId());
    }

    private void handleVerifiedReportStrike(Report report, ReportStatus previousStatus) {
        if (report.getStatus() != ReportStatus.ACTION_TAKEN || previousStatus == ReportStatus.ACTION_TAKEN) {
            return;
        }
        long verifiedCount = reportRepository.countByReporteeUserIdAndCategoryAndStatus(
                report.getReportee().getUserId(), report.getCategory(), ReportStatus.ACTION_TAKEN);

        if (verifiedCount == 1) {
            notificationService.createNotification(
                    report.getReportee().getUserId(),
                    "Cảnh báo hành vi",
                    "Admin đã xác nhận một báo cáo thuộc nhóm " + report.getCategory().name()
                            + ". Vui lòng điều chỉnh hành vi để tránh bị xem xét khóa tài khoản.",
                    NotificationType.REPORT,
                    "REPORT-" + report.getReportId());
        }

        if (verifiedCount == VERIFIED_REPORT_REVIEW_THRESHOLD) {
            notifyAdminsAboutVerifiedReportThreshold(report, verifiedCount);
        }
    }

    private void notifyAdminsAboutVerifiedReportThreshold(Report report, long verifiedCount) {
        String resourceId = "USER-" + report.getReportee().getUserId() + "-REPORT-" + report.getCategory().name();
        userRepository.findAllAdmins().forEach(admin ->
                notificationService.createNotification(
                        admin.getUserId(),
                        "User cần review khẩn",
                        report.getReportee().getFullName() + " đã có " + verifiedCount
                                + " báo cáo đã xác nhận cùng nhóm " + report.getCategory().name()
                                + ". Vui lòng review thủ công, không tự động khóa tài khoản.",
                        NotificationType.REPORT,
                        resourceId));
    }

    private void validateReporter(User reporter, User reportee) {
        if (reporter.getUserId().equals(reportee.getUserId())) {
            throw new BadRequestException("Bạn không thể tự báo cáo chính mình.");
        }
    }

    private void enforceDailyLimit(User reporter) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        long count = reportRepository.countByReporterUserIdAndCreatedAtBetween(reporter.getUserId(), start, end);
        if (count >= DAILY_REPORT_LIMIT) {
            throw new BadRequestException("Bạn đã đạt giới hạn báo cáo trong ngày.");
        }
    }

    private ReportContext resolveContext(CreateReportRequest request) {
        ReportContext context = new ReportContext();
        if (request.getBookingId() != null) {
            context.booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking."));
            context.stadium = context.booking.getStadium();
        }
        if (request.getJoinRequestId() != null) {
            context.joinRequest = joinRequestRepository.findByJoinId(request.getJoinRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tham gia kèo."));
            context.matchRequest = context.joinRequest.getMatchRequest();
        }
        if (request.getMatchRequestId() != null) {
            context.matchRequest = matchRequestRepository.findByMatchId(request.getMatchRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kèo ghép."));
        }
        validateJoinMatchConsistency(context);
        attachRequestedStadium(request, context);
        validateReportBoundary(request, context);
        return context;
    }

    private void validateJoinMatchConsistency(ReportContext context) {
        if (context.joinRequest == null || context.matchRequest == null) {
            return;
        }
        Integer joinMatchId = context.joinRequest.getMatchRequest().getMatchId();
        if (!joinMatchId.equals(context.matchRequest.getMatchId())) {
            throw new BadRequestException("Yêu cầu tham gia không thuộc kèo ghép đã chọn.");
        }
    }

    private void attachRequestedStadium(CreateReportRequest request, ReportContext context) {
        if (request.getStadiumId() == null) {
            return;
        }
        Stadium requested = stadiumRepository.findById(request.getStadiumId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        if (context.stadium != null && !context.stadium.getStadiumId().equals(requested.getStadiumId())) {
            throw new BadRequestException("Sân không khớp với ngữ cảnh báo cáo.");
        }
        context.stadium = requested;
    }

    private void validateReportBoundary(CreateReportRequest request, ReportContext context) {
        if (!context.hasAnyContext()) {
            throw new BadRequestException("Bao cao phai gan voi booking, keo ghep hoac san.");
        }
        if (context.hasOnlyStadiumContext() && request.getCategory() != ReportCategory.FAKE_LISTING) {
            throw new BadRequestException(
                    "Bao cao chi gan san chi danh cho niem yet gia. Van de chat luong/dich vu can tao khieu nai booking.");
        }
    }

    private void validateRealInteraction(User reporter, User reportee, ReportContext context) {
        boolean valid = hasBookingInteraction(reporter, reportee, context.booking)
                || hasMatchInteraction(reporter, reportee, context)
                || hasStadiumListingInteraction(reporter, reportee, context);
        if (!valid) {
            throw new BadRequestException("Reporter và reportee phải có tương tác thực trong booking hoặc kèo ghép.");
        }
    }

    private boolean hasBookingInteraction(User reporter, User reportee, Booking booking) {
        if (booking == null) {
            return false;
        }
        Integer customerId = booking.getUser().getUserId();
        Owner owner = booking.getStadium().resolveOwner();
        Integer ownerUserId = owner != null && owner.getUser() != null ? owner.getUser().getUserId() : null;
        return samePair(reporter.getUserId(), reportee.getUserId(), customerId, ownerUserId);
    }

    private boolean hasMatchInteraction(User reporter, User reportee, ReportContext context) {
        MatchRequest match = context.matchRequest;
        if (match == null) {
            return false;
        }
        if (context.joinRequest != null && isHostJoinPair(reporter, reportee, context.joinRequest)) {
            return true;
        }
        Set<Integer> participants = getApprovedMatchParticipants(match);
        return participants.contains(reporter.getUserId()) && participants.contains(reportee.getUserId());
    }

    private boolean hasStadiumListingInteraction(User reporter, User reportee, ReportContext context) {
        if (!context.hasOnlyStadiumContext()) {
            return false;
        }
        Owner owner = context.stadium.resolveOwner();
        Integer ownerUserId = owner != null && owner.getUser() != null ? owner.getUser().getUserId() : null;
        return ownerUserId != null
                && reportee.getUserId().equals(ownerUserId)
                && !reporter.getUserId().equals(ownerUserId);
    }

    private boolean isHostJoinPair(User reporter, User reportee, JoinRequest joinRequest) {
        Integer hostId = joinRequest.getMatchRequest().getUser().getUserId();
        Integer joinUserId = joinRequest.getUser().getUserId();
        return samePair(reporter.getUserId(), reportee.getUserId(), hostId, joinUserId);
    }

    private Set<Integer> getApprovedMatchParticipants(MatchRequest match) {
        Set<Integer> participants = new LinkedHashSet<>();
        participants.add(match.getUser().getUserId());
        joinRequestRepository.findAllByMatchRequestMatchId(match.getMatchId()).stream()
                .filter(join -> join.getRequestStatus() == JoinRequestStatus.APPROVED)
                .map(join -> join.getUser().getUserId())
                .forEach(participants::add);
        return participants;
    }

    private boolean samePair(Integer leftA, Integer leftB, Integer rightA, Integer rightB) {
        return rightA != null && rightB != null
                && ((leftA.equals(rightA) && leftB.equals(rightB))
                || (leftA.equals(rightB) && leftB.equals(rightA)));
    }

    private List<String> cleanEvidenceUrls(List<String> evidenceUrls) {
        if (evidenceUrls == null) {
            return new ArrayList<>();
        }
        return evidenceUrls.stream()
                .map(this::cleanText)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + email));
    }

    private Report findReport(Integer reportId) {
        return reportRepository.findWithDetailsByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo."));
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reporter(toUserSummary(report.getReporter()))
                .reportee(toUserSummary(report.getReportee()))
                .bookingId(report.getBooking() != null ? report.getBooking().getBookingId() : null)
                .matchRequestId(report.getMatchRequest() != null ? report.getMatchRequest().getMatchId() : null)
                .joinRequestId(report.getJoinRequest() != null ? report.getJoinRequest().getJoinId() : null)
                .stadiumId(report.getStadium() != null ? report.getStadium().getStadiumId() : null)
                .stadiumName(report.getStadium() != null ? report.getStadium().getStadiumName() : null)
                .category(report.getCategory())
                .description(report.getDescription())
                .evidenceUrls(report.getEvidenceUrls() != null ? List.copyOf(report.getEvidenceUrls()) : List.of())
                .status(report.getStatus())
                .resolvedBy(toUserSummary(report.getResolvedBy()))
                .resolvedAt(report.getResolvedAt())
                .resolutionNote(report.getResolutionNote())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private ReportResponse.UserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return ReportResponse.UserSummary.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();
    }

    private static class ReportContext {
        private Booking booking;
        private MatchRequest matchRequest;
        private JoinRequest joinRequest;
        private Stadium stadium;

        private boolean hasAnyContext() {
            return booking != null || matchRequest != null || joinRequest != null || stadium != null;
        }

        private boolean hasOnlyStadiumContext() {
            return stadium != null && booking == null && matchRequest == null && joinRequest == null;
        }
    }
}
