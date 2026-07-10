package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.RefundExceptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response trả về cho mọi actor (Customer / Owner / Admin) khi thao tác với RefundExceptionRequest.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundExceptionResponse {

    private Integer requestId;
    private Integer bookingId;

    /** Thông tin đơn đặt sân tóm tắt. */
    private String stadiumName;
    private String reservationDate;

    private Integer customerId;
    private String customerName;

    private String reason;
    private String evidenceUrl;
    private RefundExceptionStatus status;

    private String ownerNote;
    private String adminNote;

    /** Tỷ lệ hoàn tiền đã được chấp thuận (50 hoặc 100). null nếu chưa duyệt / bị từ chối. */
    private Integer refundPercent;

    private LocalDateTime createdAt;
    private LocalDateTime ownerReviewedAt;
    private LocalDateTime adminReviewedAt;
    private LocalDateTime expiresAt;

    /**
     * true nếu khách có thể bấm "Leo thang Admin" — chỉ khi status = REJECTED_OWNER
     * và request chưa quá expiresAt.
     */
    private boolean canEscalate;

    /** true nếu đã quá thời hạn expiresAt. */
    private boolean isExpired;
}
