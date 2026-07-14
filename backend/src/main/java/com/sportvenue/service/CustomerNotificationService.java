package com.sportvenue.service;

import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.Review;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerNotificationService {

    // Query
    PageResponse<NotificationResponse> getNotifications(Integer userId, Boolean unreadOnly, Pageable pageable);

    long countUnread(Integer userId);

    List<NotificationResponse> getRecentNotifications(Integer userId, int limit);

    // Mutation
    void markAsRead(Integer userId, List<Long> notificationIds);

    void markAllAsRead(Integer userId);

    // Trigger notifications
    void notifyBookingConfirmed(Integer customerId, Booking booking);

    void notifyBookingCancelled(Integer customerId, Booking booking, String reason);

    void notifyPaymentReceived(Integer customerId, Payment payment);

    void notifyPaymentFailed(Integer customerId, Payment payment);

    void notifyRefundProcessed(Integer customerId, Payment refund, BigDecimal amount);

    void notifyRefundExceptionDecision(Integer customerId, RefundExceptionRequest exception, boolean approved);

    void notifyComplaintAcknowledged(Integer customerId, Complaint complaint);

    void notifyComplaintOwnerReplied(Integer customerId, Complaint complaint, String replyText);

    void notifyComplaintResolved(Integer customerId, Complaint complaint, String resolution);

    void notifyComplaintEscalated(Integer customerId, Complaint complaint);

    void notifyReviewReminder(Integer customerId, Booking booking);

    void notifyReviewOwnerResponded(Integer customerId, Review review, String ownerResponse);

    void notifyMatchRequestReceived(Integer customerId, JoinRequest request);

    void notifyMatchRequestApproved(Integer customerId, JoinRequest request);

    void notifyMatchRequestRejected(Integer customerId, JoinRequest request);

    void notifyMatchCancelled(Integer customerId, MatchRequest match);

    void notifyUpgradeApproved(Integer customerId, Owner owner);

    void notifyUpgradeRejected(Integer customerId, String reason);

    void notifyAccountLocked(Integer customerId, String reason);

    void notifyAccountUnlocked(Integer customerId);

}
