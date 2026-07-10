package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private Integer bookingId;
    private String stadiumName;
    private String customerName;
    private LocalDateTime playTime;
    private BigDecimal originalPrice;
    private BigDecimal serviceFee;
    private BigDecimal refundAmount;
    private Integer refundPercentage; // 100, 50, hoặc 0
    private String bookingStatus;      // CANCELLED
    private String paymentStatus;      // REFUNDED
    private LocalDateTime processedAt;
    private String reason;
}
