package com.sportvenue.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tổng hợp Gross/Refund/Fee/Net cho Admin Booking Stats.
 * Nguồn sự thật:
 *   - Gross  = Payment (amount > 0, SUCCESS)
 *   - Refund = Payment (amount < 0, SUCCESS), giá trị dương
 *   - Fee    = Platform Wallet (SERVICE_FEE_CREDIT)
 *   - Net    = Gross - Refund - Fee
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingStatsResponse {
    private long totalBookings;
    /** Gross = tổng tiền thực thu (Payment > 0). */
    private BigDecimal totalGMV;
    /** Refund = tổng tiền đã hoàn (Payment < 0, giá trị dương). */
    private BigDecimal totalRefund;
    /** Fee = phí Platform thực thu (Platform Wallet SERVICE_FEE_CREDIT). */
    private BigDecimal totalServiceFee;
    /** Net = Gross - Refund - Fee. */
    private BigDecimal totalNet;
}

