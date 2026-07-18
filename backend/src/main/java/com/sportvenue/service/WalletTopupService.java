package com.sportvenue.service;

import com.sportvenue.dto.request.WalletTopupRequest;
import com.sportvenue.dto.response.VnpayIpnResponse;
import com.sportvenue.dto.response.WalletTopupResponse;
import com.sportvenue.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;

/**
 * Nạp tiền vào ví Customer qua VNPay — độc lập với Booking/Slot, dùng chung cổng VNPay
 * và {@code PaymentReturnController} với {@link PaymentService} nhưng tra cứu trên
 * {@code wallet_topups} thay vì {@code payments}. Phân biệt bằng tiền tố {@link #TXN_REF_PREFIX}
 * trên {@code vnp_TxnRef}.
 */
public interface WalletTopupService {

    /** Tiền tố txnRef để phân biệt giao dịch nạp ví với giao dịch thanh toán booking. */
    String TXN_REF_PREFIX = "TOPUP-";

    /**
     * Kiểm tra 1 request callback VNPay có phải dành cho luồng nạp ví hay không (dựa vào
     * {@code vnp_TxnRef}), dùng ở {@code PaymentReturnController} để routing trước khi xử lý.
     */
    boolean isTopupCallback(HttpServletRequest request);

    /**
     * Sinh paymentUrl VNPay cho yêu cầu nạp tiền vào ví của customer đang đăng nhập.
     */
    WalletTopupResponse initiateTopup(UserPrincipal principal, WalletTopupRequest request, HttpServletRequest httpRequest);

    /**
     * Xử lý callback return (browser redirect) cho giao dịch nạp ví.
     */
    TopupReturnResult handleTopupReturn(HttpServletRequest request);

    /**
     * Xử lý callback IPN (server-to-server) cho giao dịch nạp ví — không bao giờ ném exception,
     * luôn trả {@link VnpayIpnResponse} đúng chuẩn VNPay.
     */
    VnpayIpnResponse handleTopupIpn(HttpServletRequest request);

    /**
     * Kết quả xử lý callback nạp ví — controller dùng để redirect browser về trang Ví ở frontend.
     */
    record TopupReturnResult(boolean success, BigDecimal amount, String reason) {
    }
}
