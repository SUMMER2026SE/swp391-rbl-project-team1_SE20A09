package com.sportvenue.entity;

import com.sportvenue.entity.enums.RefundExceptionStatus;
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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng refund_exception_requests (Mục 1.6 P0).
 *
 * <p>Luồng xét duyệt ngoại lệ hoàn tiền: khách hủy đơn trong vòng 12h (hoàn 0%)
 * có thể xin xem xét lại vì lý do bất khả kháng. Owner duyệt trước (SLA 48h),
 * nếu từ chối khách leo thang lên Admin quyết định.</p>
 */
@Entity
@Table(name = "refund_exception_requests", indexes = {
        @Index(name = "idx_refund_exception_booking", columnList = "booking_id"),
        @Index(name = "idx_refund_exception_status", columnList = "status"),
        @Index(name = "idx_refund_exception_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundExceptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    /** Đơn đặt sân đã bị hủy (bookingStatus = CANCELLED). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Khách hàng gửi yêu cầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** Lý do bất khả kháng do khách điền (bắt buộc). */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** URL bằng chứng đính kèm (tuỳ chọn — ảnh giấy nhập viện, v.v.). */
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    /** Trạng thái xử lý yêu cầu. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RefundExceptionStatus status = RefundExceptionStatus.PENDING_OWNER;

    /** Ghi chú / lý do từ Owner khi duyệt hoặc từ chối. */
    @Column(name = "owner_note", columnDefinition = "TEXT")
    private String ownerNote;

    /** Ghi chú / lý do từ Admin khi ra quyết định cuối. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /**
     * Tỷ lệ hoàn tiền được chấp thuận (50 hoặc 100).
     * NULL khi yêu cầu bị từ chối hoặc chưa xử lý.
     */
    @Column(name = "refund_percent")
    private Integer refundPercent;

    /** Thời điểm khách tạo yêu cầu. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Thời điểm Owner phản hồi (APPROVED/REJECTED). */
    @Column(name = "owner_reviewed_at")
    private LocalDateTime ownerReviewedAt;

    /** Thời điểm Admin ra quyết định cuối. */
    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;

    /**
     * Thời hạn tối đa để gửi/xử lý yêu cầu này (createdAt + 72h).
     * Sau mốc này Scheduler sẽ set status = EXPIRED.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
