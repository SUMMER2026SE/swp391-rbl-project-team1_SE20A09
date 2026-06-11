package com.sportvenue.entity;

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

import java.io.Serializable;
import java.time.LocalDateTime;

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

    /** Trạng thái xử lý khiếu nại. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /** Phản hồi của Owner sau khi xử lý khiếu nại. */
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
