package com.sportvenue.service;

import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.exception.PaymentGatewayRefundException;
import com.sportvenue.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

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
    void processRefund_Success_WhenAmountGreaterThanZero() {
        // Arrange
        BigDecimal refundAmount = new BigDecimal("50000");

        // Act & Assert
        assertDoesNotThrow(() -> paymentService.processRefund(payment, refundAmount, "Test Refund"));
    }

    @Test
    void processRefund_ThrowsException_WhenAmountIsZero() {
        // Arrange
        BigDecimal refundAmount = BigDecimal.ZERO;

        // Act & Assert
        PaymentGatewayRefundException exception = assertThrows(
                PaymentGatewayRefundException.class,
                () -> paymentService.processRefund(payment, refundAmount, "Test Refund Zero")
        );
        assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
    }

    @Test
    void processRefund_ThrowsException_WhenAmountIsNegative() {
        // Arrange
        BigDecimal refundAmount = new BigDecimal("-10000");

        // Act & Assert
        PaymentGatewayRefundException exception = assertThrows(
                PaymentGatewayRefundException.class,
                () -> paymentService.processRefund(payment, refundAmount, "Test Refund Negative")
        );
        assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
    }
}
