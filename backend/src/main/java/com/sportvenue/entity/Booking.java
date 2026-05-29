package com.sportvenue.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng bookings.
 * Đại diện cho một lần đặt sân của khách hàng.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user_id", columnList = "user_id"),
        @Index(name = "idx_bookings_stadium_id", columnList = "stadium_id"),
        @Index(name = "idx_bookings_status", columnList = "booking_status")
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
}
