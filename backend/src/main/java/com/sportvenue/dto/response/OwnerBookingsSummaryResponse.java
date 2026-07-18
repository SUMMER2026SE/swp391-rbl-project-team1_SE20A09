package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tổng hợp doanh thu Gross/Refund/Fee/Net trên TOÀN BỘ booking của Owner (không phụ thuộc phân
 * trang) — dùng cho card summary ở /owner/bookings, tránh bug tổng bị lệch khi chỉ cộng dồn
 * booking đang hiển thị trên 1 trang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerBookingsSummaryResponse {
    private BigDecimal grossAmount;
    private BigDecimal refundedAmount;
    private BigDecimal serviceFee;
    private BigDecimal netAmount;
}
