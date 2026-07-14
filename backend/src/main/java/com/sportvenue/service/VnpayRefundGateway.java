package com.sportvenue.service;

import com.sportvenue.entity.Payment;

import java.math.BigDecimal;

/**
 * Cổng gọi API Refund/QueryDR thật của VNPay — tách riêng khỏi {@link PaymentService} để có thể
 * đổi giữa implementation mock (chưa có credential VNPay hỗ trợ Refund/QueryDR) và implementation
 * gọi HTTP thật sau này chỉ bằng cấu hình ({@code vnpay.refund.mock-enabled}), không cần sửa
 * {@code PaymentServiceImpl}/{@code RefundServiceImpl}.
 */
public interface VnpayRefundGateway {

    /**
     * Gọi API hoàn tiền sang cổng thanh toán.
     * @throws com.sportvenue.exception.PaymentGatewayRefundException nếu cổng thanh toán từ chối/lỗi
     */
    void refund(Payment originalPayment, BigDecimal refundAmount, String refundReason);

    /**
     * Truy vấn trạng thái giao dịch hoàn tiền (QueryDR).
     * @return true nếu thành công, false nếu thất bại.
     */
    boolean queryRefundStatus(Payment refundPayment);
}
