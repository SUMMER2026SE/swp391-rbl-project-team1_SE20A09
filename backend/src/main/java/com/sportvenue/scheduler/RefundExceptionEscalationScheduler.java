package com.sportvenue.scheduler;

import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.enums.RefundExceptionStatus;
import com.sportvenue.repository.RefundExceptionRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-CUS-1.6 P0: Tự động leo thang yêu cầu ngoại lệ khi Owner không phản hồi trong 48h (SLA).
 *
 * <p>Copy pattern từ {@link BookingExpiryScheduler}: dùng {@code fixedDelay} để tránh overlap,
 * quét mỗi 5 phút và chuyển {@code PENDING_OWNER} → {@code PENDING_ADMIN} cho các request
 * đã quá 48h mà Owner chưa xử lý.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundExceptionEscalationScheduler {

    private static final int OWNER_SLA_HOURS = 48;

    private final RefundExceptionRepository refundExceptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Quét mỗi 5 phút: tìm request {@code PENDING_OWNER} đã tạo trước 48h → leo thang Admin.
     * Dùng {@code fixedDelay} (không phải {@code fixedRate}) để tránh overlap nếu batch lớn.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void escalateExpiredOwnerReviews() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(OWNER_SLA_HOURS);
        List<RefundExceptionRequest> expired = refundExceptionRepository
                .findByStatusAndCreatedAtBefore(RefundExceptionStatus.PENDING_OWNER, deadline);

        if (expired.isEmpty()) {
            return;
        }

        List<User> admins = userRepository.findAllAdmins();

        for (RefundExceptionRequest req : expired) {
            req.setStatus(RefundExceptionStatus.PENDING_ADMIN);
            req.setOwnerNote("[Tự động leo thang: Owner không phản hồi trong " + OWNER_SLA_HOURS + "h]");

            // Notify tất cả Admin
            admins.forEach(admin ->
                    notificationService.publishNotificationEvent(
                            admin.getUserId(),
                            "Yêu cầu ngoại lệ leo thang tự động",
                            "Yêu cầu #" + req.getRequestId() + " (đơn #"
                                    + req.getBooking().getBookingId()
                                    + ") chờ Owner phản hồi quá " + OWNER_SLA_HOURS + "h — đã leo thang Admin.",
                            NotificationType.SYSTEM,
                            String.valueOf(req.getRequestId())
                    )
            );

            // Notify Customer
            try {
                notificationService.publishNotificationEvent(
                        req.getCustomer().getUserId(),
                        "Yêu cầu của bạn đã được chuyển lên Admin",
                        "Owner chưa phản hồi sau " + OWNER_SLA_HOURS + "h — yêu cầu #"
                                + req.getRequestId() + " đã được tự động chuyển lên Admin xem xét.",
                        NotificationType.BOOKING,
                        String.valueOf(req.getRequestId())
                );
            } catch (Exception e) {
                log.warn("[EscalationScheduler] Failed to notify customer for requestId={}", req.getRequestId(), e);
            }
        }

        refundExceptionRepository.saveAll(expired);
        log.info("⏰ [EscalationScheduler] {} request(s) leo thang Admin do Owner không phản hồi trong {}h (deadline={})",
                expired.size(), OWNER_SLA_HOURS, deadline);
    }
}
