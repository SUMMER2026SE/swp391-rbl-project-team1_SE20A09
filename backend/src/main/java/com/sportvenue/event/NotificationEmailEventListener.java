package com.sportvenue.event;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.User;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @EventListener
    @Async
    public void handleNotificationEmail(NotificationEmailEvent event) {
        try {
            User customer = userRepository.findById(event.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            String customerName = customer.getFullName() != null ? customer.getFullName() : "Khách hàng";
            String toEmail = customer.getEmail();
            Object entity = event.getRelatedEntity();

            switch (event.getEventType()) {
                case "BOOKING_CONFIRMED":
                    if (entity instanceof Booking booking) {
                        emailService.sendBookingConfirmationEmail(
                                toEmail, customerName, booking.getStadium().getStadiumName(),
                                booking.getBookingId(), booking.getReservationDate(),
                                booking.getSlot(), booking.getTotalPrice()
                        );
                    }
                    break;
                case "BOOKING_CANCELLED":
                    if (entity instanceof Object[] arr && arr.length == 2 && arr[0] instanceof Booking booking && arr[1] instanceof String reason) {
                        emailService.sendBookingCancellationEmail(
                                toEmail, customerName, booking.getStadium().getStadiumName(),
                                booking.getBookingId(), reason, "Hệ thống/Chủ sân" // TODO: adjust cancelledBy if needed
                        );
                    }
                    break;
                case "PAYMENT_RECEIVED":
                    if (entity instanceof Payment payment) {
                        emailService.sendPaymentConfirmedEmail(toEmail, customerName, payment.getBooking().getBookingId(), payment.getBooking().getStadium().getStadiumName(), payment.getAmount());
                    }
                    break;
                case "PAYMENT_FAILED":
                    if (entity instanceof Payment payment) {
                        emailService.sendPaymentFailedEmail(toEmail, customerName, payment.getBooking().getBookingId(), payment.getBooking().getStadium().getStadiumName());
                    }
                    break;
                case "REFUND_PROCESSED":
                    if (entity instanceof Object[] arr && arr.length == 2 && arr[0] instanceof Payment refund && arr[1] instanceof BigDecimal amount) {
                        emailService.sendRefundEmail(toEmail, customerName, refund.getBooking().getStadium().getStadiumName(), refund.getBooking().getBookingId(), amount, 100, refund.getBooking().getTotalPrice());
                    }
                    break;
                case "REFUND_EXCEPTION_DECISION":
                    if (entity instanceof Object[] arr && arr.length == 2 && arr[0] instanceof RefundExceptionRequest exception && arr[1] instanceof Boolean approved) {
                        String reason = exception.getAdminNote() != null ? exception.getAdminNote() : exception.getOwnerNote();
                        emailService.sendRefundExceptionDecisionEmail(toEmail, customerName, exception.getBooking().getBookingId(), approved, reason);
                    }
                    break;
                case "COMPLAINT_ACKNOWLEDGED":
                    if (entity instanceof Complaint complaint) {
                        emailService.sendComplaintAcknowledgedEmail(toEmail, customerName, complaint.getComplaintId(), complaint.getContent());
                    }
                    break;
                case "COMPLAINT_OWNER_REPLIED":
                    if (entity instanceof Object[] arr && arr.length == 2 && arr[0] instanceof Complaint complaint && arr[1] instanceof String reply) {
                        emailService.sendComplaintOwnerRepliedEmail(toEmail, customerName, complaint.getComplaintId(), reply);
                    }
                    break;
                case "COMPLAINT_RESOLVED":
                    if (entity instanceof Object[] arr && arr.length == 2 && arr[0] instanceof Complaint complaint && arr[1] instanceof String resolution) {
                        emailService.sendComplaintResolvedEmail(toEmail, customerName, complaint.getComplaintId(), resolution);
                    }
                    break;
                case "REVIEW_REMINDER":
                    if (entity instanceof Booking booking) {
                        emailService.sendReviewRequestEmail(toEmail, customerName, booking.getStadium().getStadiumName(), booking.getBookingId(), booking.getReservationDate());
                    }
                    break;
                case "REVIEW_OWNER_RESPONDED":
                    if (entity instanceof Object[] arr && arr.length == 2
                            && arr[0] instanceof com.sportvenue.entity.Review review
                            && arr[1] instanceof String ownerResp) {
                        emailService.sendReviewOwnerResponseEmail(
                                toEmail, customerName,
                                review.getStadium().getStadiumName(),
                                review.getBooking().getBookingId(),
                                ownerResp);
                    }
                    break;
                case "MATCH_REQUEST_RECEIVED":
                    if (entity instanceof JoinRequest request) {
                        emailService.sendMatchRequestReceivedEmail(toEmail, request.getMatchRequest().getUser().getFullName(), request.getUser().getFullName(), request.getMatchRequest().getStartTime().toString());
                    }
                    break;
                case "MATCH_REQUEST_APPROVED":
                    if (entity instanceof JoinRequest request) {
                        emailService.sendMatchRequestApprovedEmail(toEmail, request.getUser().getFullName(), request.getMatchRequest().getUser().getFullName(), request.getMatchRequest().getStartTime().toString());
                    }
                    break;
                case "MATCH_REQUEST_REJECTED":
                    if (entity instanceof JoinRequest request) {
                        emailService.sendMatchRequestRejectedEmail(toEmail, request.getUser().getFullName(), request.getMatchRequest().getUser().getFullName(), request.getMatchRequest().getStartTime().toString());
                    }
                    break;
                case "MATCH_CANCELLED":
                    if (entity instanceof MatchRequest request) {
                        emailService.sendMatchCancelledEmail(toEmail, "Người chơi", request.getUser().getFullName(), request.getStartTime().toString());
                    }
                    break;
                case "UPGRADE_APPROVED":
                    if (entity instanceof Owner owner) {
                        emailService.sendOwnerRegistrationSuccessEmail(toEmail, customerName, owner.getBusinessName());
                    }
                    break;
                case "UPGRADE_REJECTED":
                    if (entity instanceof String reason) {
                        emailService.sendUpgradeRejectedEmail(toEmail, customerName, reason);
                    }
                    break;
                case "ACCOUNT_LOCKED":
                    if (entity instanceof String reason) {
                        emailService.sendAccountLockedEmail(toEmail, customerName, reason);
                    }
                    break;
                case "ACCOUNT_UNLOCKED":
                    emailService.sendAccountUnlockedEmail(toEmail, customerName);
                    break;
                default:
                    log.warn("Unhandled email event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process email event: {}", event.getEventType(), e);
        }
    }
}
