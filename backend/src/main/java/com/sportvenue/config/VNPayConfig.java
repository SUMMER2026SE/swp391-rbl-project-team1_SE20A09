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

    @Value("${vnpay.url}")
    private String url;

    @Value("${vnpay.return-url}")
    private String returnUrl;

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

    @PostConstruct
    void warnIfMissing() {
        if (tmnCode == null || tmnCode.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            org.slf4j.LoggerFactory.getLogger(VNPayConfig.class)
                    .warn("VNPay credentials (tmn-code / hash-secret) chưa được cấu hình — "
                            + "thanh toán VNPay sẽ thất bại cho đến khi đặt VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env");
        }
    }
}
