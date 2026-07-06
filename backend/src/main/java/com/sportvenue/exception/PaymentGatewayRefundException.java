package com.sportvenue.exception;

public class PaymentGatewayRefundException extends RuntimeException {
    public PaymentGatewayRefundException(String message) {
        super(message);
    }
    public PaymentGatewayRefundException(String message, Throwable cause) {
        super(message, cause);
    }
}
