package com.sportvenue.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;

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
 * Entity ánh xạ bảng bookings.
 * Đại diện cho một lần đặt sân của khách hàng.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user_id", columnList = "user_id"),
        @Index(name = "idx_bookings_stadium_id", columnList = "stadium_id"),
        @Index(name = "idx_bookings_status", columnList = "booking_status"),
        @Index(name = "idx_bookings_recurring_group_id", columnList = "recurring_group_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    /** Khách hàng đặt sân. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Sân được đặt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    /** Khung giờ được đặt — liên kết 1-1 để tránh double-booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    /** Tổng tiền thanh toán (có thể tính sau khi áp dụng khuyến mãi). */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Trạng thái đơn đặt sân — cần Owner xác nhận. */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    /** Trạng thái thanh toán — độc lập với booking status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /** Thời điểm tạo đơn đặt. */
    @Column(name = "booking_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime bookingDate = LocalDateTime.now();

    /** Ghi chú thêm của khách hàng khi đặt sân. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "address_text", length = 255)
    private String addressText;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /** Ngày khách hàng thực sự ra sân chơi. */
    @Column(name = "reservation_date", nullable = false)
    private java.time.LocalDate reservationDate;

    /**
     * Mã định danh chuỗi đặt sân định kỳ (UC-CUS-01) — UUID v4 dạng chuỗi.
     * NULL cho đơn đặt đơn lẻ; cùng giá trị cho tất cả đơn thuộc một chuỗi recurring.
     */
    @Column(name = "recurring_group_id", length = 36)
    private String recurringGroupId;

    /**
     * UC-CUS-01: Thời điểm hết hạn giữ sân (chỉ áp dụng cho {@code PENDING_PAYMENT}).
     * Scheduler sẽ tự huỷ booking khi {@code expiredAt < now()}. NULL khi đã thanh toán
     * (CONFIRMED) hoặc huỷ (CANCELLED).
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * UC-CUS-03: Lý do khách hàng hoặc chủ sân hủy đơn (nullable, tối đa 255 ký tự).
     * Tương ứng cột {@code cancel_reason} trong bảng bookings — được thêm bởi migration V5.10.
     */
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    /**
     * Thời điểm đã gửi nhắc lịch chơi cho khách.
     * NULL = chưa nhắc, NOT NULL = đã nhắc — chống duplicate notification.
     * Được thêm bởi migration V5.11, dùng bởi {@code BookingReminderScheduler}.
     */
    @Column(name = "reminder_sent_at")
    private java.time.LocalDateTime reminderSentAt;
}
