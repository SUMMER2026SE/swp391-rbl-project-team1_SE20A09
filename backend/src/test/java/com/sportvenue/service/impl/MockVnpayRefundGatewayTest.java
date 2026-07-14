package com.sportvenue.service.impl;

import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.exception.PaymentGatewayRefundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cho implementation mock của {@link com.sportvenue.service.VnpayRefundGateway} — logic mock
 * này trước đây nằm trực tiếp trong {@code PaymentServiceImpl.processRefund}, đã chuyển sang đây
 * khi tách interface (xem C-prep 2 trong kế hoạch hoàn thiện luồng thanh toán).
 */
class MockVnpayRefundGatewayTest {

    private final MockVnpayRefundGateway gateway = new MockVnpayRefundGateway();

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .paymentId(1)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionCode("VNP123456")
                .amount(new BigDecimal("100000"))
                .build();
    }

    @Test
    void refund_Success_WhenAmountGreaterThanZero() {
        BigDecimal refundAmount = new BigDecimal("50000");

        assertDoesNotThrow(() -> gateway.refund(payment, refundAmount, "Test Refund"));
    }

    @Test
    void refund_ThrowsException_WhenAmountIsZero() {
        BigDecimal refundAmount = BigDecimal.ZERO;

        PaymentGatewayRefundException exception = assertThrows(
                PaymentGatewayRefundException.class,
                () -> gateway.refund(payment, refundAmount, "Test Refund Zero")
        );
        assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
    }

    @Test
    void refund_ThrowsException_WhenAmountIsNegative() {
        BigDecimal refundAmount = new BigDecimal("-10000");

        PaymentGatewayRefundException exception = assertThrows(
                PaymentGatewayRefundException.class,
                () -> gateway.refund(payment, refundAmount, "Test Refund Negative")
        );
        assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
    }

    @Test
    void queryRefundStatus_ReturnsTrue() {
        assertTrue(gateway.queryRefundStatus(payment));
    }
}
