package com.sportvenue.service.impl;

import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.event.NotificationEmailEvent;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.CustomerNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationServiceImpl implements CustomerNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final List<NotificationType> CUSTOMER_TYPES = Arrays.asList(
            NotificationType.BOOKING_CONFIRMED,
            NotificationType.BOOKING_CANCELLED,
            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.REFUND_PROCESSED,
            NotificationType.REFUND_EXCEPTION_DECISION,
            NotificationType.COMPLAINT_ACKNOWLEDGED,
            NotificationType.COMPLAINT_OWNER_REPLIED,
            NotificationType.COMPLAINT_RESOLVED,
            NotificationType.COMPLAINT_ESCALATED,
            NotificationType.REVIEW_REMINDER,
            NotificationType.REVIEW_OWNER_RESPONDED,
            NotificationType.MATCH_REQUEST_RECEIVED,
            NotificationType.MATCH_REQUEST_APPROVED,
            NotificationType.MATCH_REQUEST_REJECTED,
            NotificationType.MATCH_CANCELLED,
            NotificationType.UPGRADE_APPROVED,
            NotificationType.UPGRADE_REJECTED,
            NotificationType.ACCOUNT_LOCKED,
            NotificationType.ACCOUNT_UNLOCKED
    );

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Integer userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByUserUserIdAndIsRead(userId, false, pageable);
        } else {
            page = notificationRepository.findCustomerNotifications(userId, CUSTOMER_TYPES, pageable);
        }
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        return notificationRepository.countCustomerUnread(userId, CUSTOMER_TYPES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Integer userId, int limit) {
        List<Notification> list = notificationRepository.findCustomerNotificationsByType(userId, CUSTOMER_TYPES);
        return list.stream()
                .limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Integer userId, List<Long> notificationIds) {
        notificationRepository.markIdsAsRead(userId, notificationIds);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAdminNotificationsAsRead(userId, CUSTOMER_TYPES);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .relatedResourceId(notification.getRelatedResourceId())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void saveAndPublish(Integer customerId, NotificationType type, String title, String message, String relatedId, Object relatedEntity) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Notification notification = Notification.builder()
                .user(customer)
                .notificationType(type)
                .title(title)
                .message(message)
                .relatedResourceId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationEmailEvent(this, customerId, type.name(), relatedEntity));
    }

    @Override
    @Transactional
    public void notifyBookingConfirmed(Integer customerId, Booking booking) {
        saveAndPublish(customerId, NotificationType.BOOKING_CONFIRMED,
                "Đặt sân thành công!",
                "Đơn đặt sân #" + booking.getBookingId() + " đã được xác nhận",
                String.valueOf(booking.getBookingId()),
                booking);
    }

    @Override
    @Transactional
    public void notifyBookingCancelled(Integer customerId, Booking booking, String reason) {
        saveAndPublish(customerId, NotificationType.BOOKING_CANCELLED,
                "Đơn đặt sân bị hủy",
                "Đơn đặt sân #" + booking.getBookingId() + " đã bị hủy. Lý do: " + reason,
                String.valueOf(booking.getBookingId()),
                new Object[]{booking, reason});
    }

    @Override
    @Transactional
    public void notifyPaymentReceived(Integer customerId, Payment payment) {
        saveAndPublish(customerId, NotificationType.PAYMENT_RECEIVED,
                "Thanh toán thành công",
                "Thanh toán cho đơn đặt sân #" + payment.getBooking().getBookingId() + " đã được ghi nhận",
                String.valueOf(payment.getBooking().getBookingId()),
                payment);
    }

    @Override
    @Transactional
    public void notifyPaymentFailed(Integer customerId, Payment payment) {
        saveAndPublish(customerId, NotificationType.PAYMENT_FAILED,
                "Thanh toán thất bại",
                "Giao dịch cho đơn đặt sân #" + payment.getBooking().getBookingId() + " đã thất bại",
                String.valueOf(payment.getBooking().getBookingId()),
                payment);
    }

    @Override
    @Transactional
    public void notifyRefundProcessed(Integer customerId, Payment refund, BigDecimal amount) {
        saveAndPublish(customerId, NotificationType.REFUND_PROCESSED,
                "Hoàn tiền thành công",
                "Số tiền " + amount + " đã được hoàn lại cho đơn #" + refund.getBooking().getBookingId(),
                String.valueOf(refund.getBooking().getBookingId()),
                new Object[]{refund, amount});
    }

    @Override
    @Transactional
    public void notifyRefundExceptionDecision(Integer customerId, RefundExceptionRequest exception, boolean approved) {
        saveAndPublish(customerId, NotificationType.REFUND_EXCEPTION_DECISION,
                approved ? "Yêu cầu hoàn tiền ngoại lệ được chấp nhận" : "Yêu cầu hoàn tiền ngoại lệ bị từ chối",
                "Yêu cầu hoàn tiền ngoại lệ cho đơn #" + exception.getBooking().getBookingId() + (approved ? " đã được duyệt" : " đã bị từ chối"),
                String.valueOf(exception.getBooking().getBookingId()),
                new Object[]{exception, approved});
    }

    @Override
    @Transactional
    public void notifyComplaintAcknowledged(Integer customerId, Complaint complaint) {
        saveAndPublish(customerId, NotificationType.COMPLAINT_ACKNOWLEDGED,
                "Khiếu nại đã được ghi nhận",
                "Khiếu nại #" + complaint.getComplaintId() + " của bạn đang được xử lý",
                String.valueOf(complaint.getComplaintId()),
                complaint);
    }

    @Override
    @Transactional
    public void notifyComplaintOwnerReplied(Integer customerId, Complaint complaint, String replyText) {
        saveAndPublish(customerId, NotificationType.COMPLAINT_OWNER_REPLIED,
                "Chủ sân đã phản hồi khiếu nại",
                "Chủ sân đã phản hồi khiếu nại #" + complaint.getComplaintId() + " của bạn",
                String.valueOf(complaint.getComplaintId()),
                new Object[]{complaint, replyText});
    }

    @Override
    @Transactional
    public void notifyComplaintResolved(Integer customerId, Complaint complaint, String resolution) {
        saveAndPublish(customerId, NotificationType.COMPLAINT_RESOLVED,
                "Khiếu nại đã được giải quyết",
                "Khiếu nại #" + complaint.getComplaintId() + " đã có kết quả xử lý",
                String.valueOf(complaint.getComplaintId()),
                new Object[]{complaint, resolution});
    }

    @Override
    @Transactional
    public void notifyReviewReminder(Integer customerId, Booking booking) {
        saveAndPublish(customerId, NotificationType.REVIEW_REMINDER,
                "Bạn có chuyến đi vừa qua",
                "Hãy dành ít phút đánh giá sân " + booking.getStadium().getStadiumName(),
                String.valueOf(booking.getBookingId()),
                booking);
    }

    @Override
    @Transactional
    public void notifyReviewOwnerResponded(Integer customerId, Review review, String ownerResponse) {
        saveAndPublish(customerId, NotificationType.REVIEW_OWNER_RESPONDED,
                "Chủ sân đã phản hồi đánh giá của bạn",
                "Chủ sân " + review.getStadium().getStadiumName() + " đã phản hồi đánh giá của bạn",
                String.valueOf(review.getReviewId()),
                new Object[]{review, ownerResponse});
    }

    @Override
    @Transactional
    public void notifyMatchRequestReceived(Integer customerId, JoinRequest request) {
        saveAndPublish(customerId, NotificationType.MATCH_REQUEST_RECEIVED,
                "Có người muốn tham gia kèo",
                "Bạn có yêu cầu tham gia kèo ghép mới",
                String.valueOf(request.getMatchRequest().getMatchId()),
                request);
    }

    @Override
    @Transactional
    public void notifyMatchRequestApproved(Integer customerId, JoinRequest request) {
        saveAndPublish(customerId, NotificationType.MATCH_REQUEST_APPROVED,
                "Yêu cầu tham gia kèo được chấp nhận",
                "Yêu cầu tham gia kèo ghép của bạn đã được duyệt",
                String.valueOf(request.getMatchRequest().getMatchId()),
                request);
    }

    @Override
    @Transactional
    public void notifyMatchRequestRejected(Integer customerId, JoinRequest request) {
        saveAndPublish(customerId, NotificationType.MATCH_REQUEST_REJECTED,
                "Yêu cầu tham gia kèo bị từ chối",
                "Yêu cầu tham gia kèo ghép của bạn đã bị từ chối",
                String.valueOf(request.getMatchRequest().getMatchId()),
                request);
    }

    @Override
    @Transactional
    public void notifyMatchCancelled(Integer customerId, MatchRequest match) {
        saveAndPublish(customerId, NotificationType.MATCH_CANCELLED,
                "Kèo ghép bị hủy",
                "Kèo ghép " + match.getTitle() + " đã bị hủy bởi chủ kèo",
                String.valueOf(match.getMatchId()),
                match);
    }

    @Override
    @Transactional
    public void notifyUpgradeApproved(Integer customerId, Owner owner) {
        saveAndPublish(customerId, NotificationType.UPGRADE_APPROVED,
                "Yêu cầu nâng cấp đối tác được duyệt",
                "Chúc mừng! Yêu cầu nâng cấp thành đối tác của bạn đã được duyệt",
                String.valueOf(owner.getOwnerId()),
                owner);
    }

    @Override
    @Transactional
    public void notifyUpgradeRejected(Integer customerId, String reason) {
        saveAndPublish(customerId, NotificationType.UPGRADE_REJECTED,
                "Yêu cầu nâng cấp đối tác bị từ chối",
                "Yêu cầu nâng cấp đối tác bị từ chối. Lý do: " + reason,
                null,
                reason);
    }

    @Override
    @Transactional
    public void notifyAccountLocked(Integer customerId, String reason) {
        saveAndPublish(customerId, NotificationType.ACCOUNT_LOCKED,
                "Tài khoản đã bị khóa",
                "Tài khoản của bạn đã bị khóa. Lý do: " + (reason == null || reason.isBlank() ? "Không có ghi chú" : reason),
                String.valueOf(customerId),
                reason);
    }

    @Override
    @Transactional
    public void notifyAccountUnlocked(Integer customerId) {
        saveAndPublish(customerId, NotificationType.ACCOUNT_UNLOCKED,
                "Tài khoản đã được mở khóa",
                "Tài khoản của bạn đã được mở khóa lại và có thể đăng nhập bình thường",
                String.valueOf(customerId),
                null);
    }
}
