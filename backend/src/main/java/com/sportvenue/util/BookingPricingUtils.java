package com.sportvenue.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Công thức tính tiền cọc dùng chung cho mọi phương thức thanh toán (VNPay, Ví) — tách ra để
 * tránh lệch số giữa các luồng nếu tỉ lệ cọc thay đổi trong tương lai.
 */
public final class BookingPricingUtils {

    /** Tỉ lệ tiền cọc trên tổng giá trị đơn — khớp với {@code PaymentServiceImpl.createVnpayPaymentUrl}. */
    public static final BigDecimal DEPOSIT_RATIO = new BigDecimal("0.3");

    private BookingPricingUtils() {
    }

    public static BigDecimal calculateDepositAmount(BigDecimal totalPrice) {
        return totalPrice.multiply(DEPOSIT_RATIO).setScale(0, RoundingMode.CEILING);
    }
}
