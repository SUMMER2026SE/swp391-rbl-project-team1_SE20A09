package com.sportvenue.controller;

import com.sportvenue.config.VNPayConfig;
import com.sportvenue.dto.response.VnpayIpnResponse;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Callback từ VNPay — KHÔNG yêu cầu đăng nhập (VNPay gọi browser redirect tới đây).
 * Endpoint này xác thực checksum, cập nhật trạng thái Payment + Booking, rồi redirect
 * browser sang trang {@code /payments/result} ở frontend.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Callback từ cổng thanh toán VNPay")
public class PaymentReturnController {

    private final PaymentService paymentService;
    private final VNPayConfig vnPayConfig;

    @GetMapping("/vnpay-return")
    @Operation(
            summary = "VNPay redirect về sau thanh toán",
            description = "Public endpoint. Xác thực checksum, cập nhật DB, redirect sang VNPAY_RETURN_URL "
                    + "với query success/bookingId/reason.")
    public RedirectView vnpayReturn(HttpServletRequest request) {
        PaymentService.ReturnResult result;
        try {
            result = paymentService.handleVnpayReturn(request);
        } catch (BadRequestException ex) {
            log.warn("VNPay return bị từ chối: {}", ex.getMessage());
            return buildRedirect(false, null, ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi không mong đợi khi xử lý VNPay return", ex);
            return buildRedirect(false, null, "internal_error");
        }
        return buildRedirect(result.success(), result.bookingId(), result.reason());
    }

    @GetMapping("/vnpay-ipn")
    @Operation(
            summary = "VNPay IPN (Instant Payment Notification)",
            description = "Public endpoint, gọi server-to-server bởi VNPay — độc lập với việc khách "
                    + "có quay lại trình duyệt hay không. LUÔN trả HTTP 200 + JSON {RspCode, Message} "
                    + "đúng chuẩn VNPay, không bao giờ redirect hay để lộ lỗi 4xx/5xx (VNPay sẽ retry "
                    + "vô hạn nếu không nhận được 200).")
    public ResponseEntity<VnpayIpnResponse> vnpayIpn(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paymentService.handleVnpayIpn(request));
        } catch (Exception ex) {
            log.error("Lỗi không mong đợi khi xử lý VNPay IPN", ex);
            return ResponseEntity.ok(VnpayIpnResponse.builder()
                    .rspCode("99")
                    .message("Unknown error")
                    .build());
        }
    }

    private RedirectView buildRedirect(boolean success, Integer bookingId, String reason) {
        // Redirect về FE (frontend-return-url) — không phải BE return-url.
        StringBuilder sb = new StringBuilder(vnPayConfig.getFrontendReturnUrl());
        sb.append(success ? "?success=true" : "?success=false");
        if (bookingId != null) {
            sb.append("&bookingId=").append(bookingId);
        }
        if (reason != null && !reason.isBlank()) {
            sb.append("&reason=").append(java.net.URLEncoder.encode(
                    reason, java.nio.charset.StandardCharsets.UTF_8));
        }
        return new RedirectView(sb.toString());
    }
}
