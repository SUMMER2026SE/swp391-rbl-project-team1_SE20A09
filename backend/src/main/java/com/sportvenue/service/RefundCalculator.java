package com.sportvenue.service;

import com.sportvenue.entity.Booking;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * UC-CUS-06: Bộ tính toán số tiền hoàn dùng chung cho cả Customer (cancel + preview)
 * và Owner (refund flow). Tách ra khỏi {@code RefundServiceImpl} để cả hai flow
 * dùng chung một quy tắc.
 *
 * <p>Quy tắc hoàn tiền dựa trên khoảng cách từ hiện tại đến giờ chơi
 * (tính theo {@code reservationDate + slot.startTime}):</p>
 * <ul>
 *   <li>{@code >= 24h} → hoàn 100%</li>
 *   <li>{@code >= 12h} → hoàn 50%</li>
 *   <li>{@code <  12h} → hoàn 0%</li>
 * </ul>
 */
@Component
public class RefundCalculator {

    /** Ngưỡng 24 giờ trước giờ chơi — hoàn 100%. */
    private static final double FULL_REFUND_HOURS = 24.0;

    /** Ngưỡng 12 giờ trước giờ chơi — hoàn 50%. */
    private static final double HALF_REFUND_HOURS = 12.0;

    /** Hệ số 50%. */
    private static final BigDecimal HALF_MULTIPLIER = new BigDecimal("0.5");

    /**
     * Tính số tiền hoàn cho booking dựa trên thời điểm hiện tại so với giờ chơi.
     *
     * @param booking booking cần tính hoàn (phải có {@code reservationDate} +
     *                {@code slot.startTime} + {@code totalPrice}).
     * @return kết quả tính gồm phần trăm và số tiền hoàn.
     */
    public RefundResult calculate(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking không được null");
        }
        if (booking.getReservationDate() == null || booking.getSlot() == null
                || booking.getSlot().getStartTime() == null) {
            throw new IllegalArgumentException(
                    "Booking thiếu reservationDate hoặc slot.startTime");
        }

        LocalDateTime playTime = LocalDateTime.of(
                booking.getReservationDate(), booking.getSlot().getStartTime());
        LocalDateTime now = LocalDateTime.now();
        double hoursDiff = (double) Duration.between(now, playTime).toMinutes() / 60.0;

        int percentage;
        BigDecimal amount;
        if (hoursDiff >= FULL_REFUND_HOURS) {
            percentage = 100;
            amount = booking.getTotalPrice();
        } else if (hoursDiff >= HALF_REFUND_HOURS) {
            percentage = 50;
            amount = booking.getTotalPrice().multiply(HALF_MULTIPLIER);
        } else {
            percentage = 0;
            amount = BigDecimal.ZERO;
        }
        return new RefundResult(percentage, amount);
    }

    /** Kết quả tính hoàn tiền — phần trăm và số tiền. */
    @Getter
    public static final class RefundResult {
        private final int percentage;
        private final BigDecimal amount;

        public RefundResult(int percentage, BigDecimal amount) {
            this.percentage = percentage;
            this.amount = amount;
        }
    }
}