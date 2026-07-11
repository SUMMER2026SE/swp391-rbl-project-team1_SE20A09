package com.sportvenue.service.impl;

import com.sportvenue.entity.Payment;
import com.sportvenue.exception.PaymentGatewayRefundException;
import com.sportvenue.service.VnpayRefundGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Implementation mock của {@link VnpayRefundGateway} — dùng khi chưa có credential VNPay hỗ trợ
 * Refund/QueryDR thật (mặc định mọi môi trường cho tới khi có credential, xem {@code VNPayConfig}).
 * KHÔNG gọi HTTP thật — chỉ giả lập độ trễ mạng và luôn trả thành công (trừ input rõ ràng sai).
 */
@Component
@ConditionalOnProperty(prefix = "vnpay.refund", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockVnpayRefundGateway implements VnpayRefundGateway {

    @Override
    public void refund(Payment originalPayment, BigDecimal refundAmount, String refundReason) {
        log.info("Bắt đầu xử lý hoàn tiền qua cổng thanh toán (Mock): Method={}, TxnRef={}, Amount={}, Reason={}",
                originalPayment.getPaymentMethod(), originalPayment.getTransactionCode(), refundAmount, refundReason);

        try {
            // TODO: Replace this mock implementation with actual HTTP Client (RestTemplate/WebClient) call to VNPay/MoMo refund API in production.
            // Giả lập delay gọi API
            Thread.sleep(500);

            // Giả lập logic kiểm tra kết quả trả về từ VNPay/MoMo
            // Trong thực tế, ở đây sẽ gửi HTTP POST request có signature đến cổng thanh toán
            boolean isSuccess = true;

            // Giả lập lỗi ngẫu nhiên hoặc dựa trên điều kiện cụ thể để test rollback nếu cần
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                isSuccess = false;
                throw new PaymentGatewayRefundException("Số tiền hoàn phải lớn hơn 0");
            }

            if (!isSuccess) {
                throw new PaymentGatewayRefundException("Cổng thanh toán từ chối yêu cầu hoàn tiền");
            }

            log.info("Hoàn tiền qua cổng thanh toán thành công (Mock): TxnRef={}", originalPayment.getTransactionCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayRefundException("Lỗi kết nối timeout tới cổng thanh toán", e);
        } catch (PaymentGatewayRefundException e) {
            log.error("Lỗi hoàn tiền qua cổng thanh toán: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không xác định khi hoàn tiền qua cổng thanh toán", e);
            throw new PaymentGatewayRefundException("Lỗi không xác định khi hoàn tiền qua cổng thanh toán", e);
        }
    }

    @Override
    public boolean queryRefundStatus(Payment refundPayment) {
        log.info("Kiểm tra trạng thái hoàn tiền (QueryDR) qua cổng thanh toán (Mock): TxnRef={}", refundPayment.getTransactionCode());
        try {
            // TODO: Call actual QueryDR API of VNPay/MoMo
            Thread.sleep(300);
            return true; // Giả sử thành công
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayRefundException("Lỗi timeout khi đối soát", e);
        }
    }
}
