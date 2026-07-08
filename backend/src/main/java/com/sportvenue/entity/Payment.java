package com.sportvenue.entity;

import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Entity ánh xạ bảng payments.
 * Lưu thông tin giao dịch thanh toán cho mỗi đơn đặt sân.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    /** Đơn đặt sân liên quan đến giao dịch này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Phương thức thanh toán được chọn. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Số tiền giao dịch (VNĐ). */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Mã giao dịch từ cổng thanh toán (VNPay/MoMo/Banking). */
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    /** Trạng thái giao dịch — được cập nhật sau khi callback từ cổng thanh toán. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus paymentStatus = TransactionStatus.PENDING;

    /** Thời điểm thanh toán thành công — null nếu chưa thanh toán. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Loại lý do hoàn tiền. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", length = 50)
    private com.sportvenue.entity.enums.RefundReasonType reasonType;

    /** Đường dẫn bằng chứng nếu lỗi do chủ sân. */
    @Column(name = "proof_url", length = 500)
    private String proofUrl;
}
