package com.sportvenue.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity ánh xạ bảng complaints.
 * Khiếu nại của khách hàng về sân hoặc dịch vụ.
 * Owner có thể phản hồi và đổi trạng thái về Resolved.
 */
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_booking_id", columnList = "booking_id"),
        @Index(name = "idx_complaints_user_id", columnList = "user_id"),
        @Index(name = "idx_complaints_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Integer complaintId;

    /** Đơn đặt sân liên quan đến khiếu nại. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Người gửi khiếu nại. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Chủ đề khiếu nại. */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /** Nội dung khiếu nại chi tiết. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Mức độ ưu tiên của khiếu nại. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    /** Trạng thái xử lý khiếu nại. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /** Phản hồi của Owner sau khi xử lý khiếu nại. */
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Thời điểm Owner resolve khiếu nại (cho 48h customer objection period) */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Hạn cuối khách hàng phản hồi (48h từ resolved_at) */
    @Column(name = "customer_response_deadline")
    private LocalDateTime customerResponseDeadline;

    /** Thời điểm được chuyển lên Admin */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    /** Lý do escalation (auto/manual) */
    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    /** Admin đã xem xét khiếu nại này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_reviewed_by")
    private User adminReviewedBy;

    /** Thời điểm Admin xem xét */
    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;

    /** Có vi phạm SLA phản hồi không */
    @Column(name = "sla_violated")
    @Builder.Default
    private Boolean slaViolated = false;
}
