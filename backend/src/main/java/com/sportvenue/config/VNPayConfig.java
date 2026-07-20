package com.sportvenue.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình VNPay sandbox — đọc từ biến môi trường qua application.yml.
 * Sử dụng cho UC-CUS-02: khách hàng thanh toán đơn đặt sân qua cổng VNPay.
 */
@Configuration
@Getter
public class VNPayConfig {

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String url;

    /**
     * Return URL mà VNPay gọi về (BE endpoint nhận vnp_* params).
     * VD: http://localhost:8080/api/v1/payments/vnpay-return.
     */
    @Value("${vnpay.return-url:http://localhost:8080/api/v1/payments/vnpay-return}")
    private String returnUrl;

    /**
     * FE URL mà BE redirect browser sang sau khi xử lý callback thành công.
     * VD: http://localhost:3000/payments/result.
     */
    @Value("${vnpay.frontend-return-url:http://localhost:3000/payments/result}")
    private String frontendReturnUrl;

    /**
     * FE URL mà BE redirect browser sang sau khi xử lý callback nạp tiền vào ví (khác trang kết
     * quả thanh toán booking vì không có bookingId để hiển thị).
     * VD: http://localhost:3000/wallet.
     */
    @Value("${vnpay.frontend-wallet-return-url:http://localhost:3000/wallet}")
    private String frontendWalletReturnUrl;

    /**
     * IPN URL — VNPay gọi server-to-server để xác nhận thanh toán.
     * Dev local: cần ngrok tunnel. VD: https://xxxx.ngrok.io/api/v1/payments/vnpay-ipn.
     */
    @Value("${vnpay.ipn-url:http://localhost:8080/api/v1/payments/vnpay-ipn}")
    private String ipnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command:pay}")
    private String command;

    @Value("${vnpay.currency-code:VND}")
    private String currencyCode;

    @Value("${vnpay.locale:vn}")
    private String locale;

    @Value("${vnpay.order-type:other}")
    private String orderType;

    /** URL API Refund/QueryDR thật của VNPay — chỉ dùng khi {@link #refundMockEnabled} = false. */
    @Value("${vnpay.refund.url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String refundUrl;

    /**
     * true (mặc định) — dùng {@code MockVnpayRefundGateway}, không gọi HTTP thật. Chỉ đổi sang
     * false khi đã có credential VNPay hỗ trợ Refund/QueryDR VÀ đã có implementation gọi HTTP thật.
     */
    @Value("${vnpay.refund.mock-enabled:true}")
    private boolean refundMockEnabled;

    @PostConstruct
    void warnIfMissing() {
        if (tmnCode == null || tmnCode.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            org.slf4j.LoggerFactory.getLogger(VNPayConfig.class)
                    .warn("VNPay credentials (tmn-code / hash-secret) chưa được cấu hình — "
                            + "thanh toán VNPay sẽ thất bại cho đến khi đặt VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env");
        }
    }
}
