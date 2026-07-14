package com.sportvenue.service;

import com.sportvenue.dto.response.VnpayIpnResponse;
import com.sportvenue.dto.response.VnpayPaymentUrlResponse;
import com.sportvenue.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

/**
 * UC-CUS-02: Thanh toán đơn đặt sân qua VNPay.
 * <ul>
 *   <li>{@link #createVnpayPaymentUrl} — sinh paymentUrl cho customer</li>
 *   <li>{@link #handleVnpayReturn} — xử lý callback return (browser redirect, có UI)</li>
 *   <li>{@link #handleVnpayIpn} — xử lý callback IPN (server-to-server, VNPay yêu cầu JSON RspCode)</li>
 * </ul>
 */
public interface PaymentService {

    /**
     * Tạo paymentUrl VNPay cho đơn đặt sân của customer.
     *
     * @throws com.sportvenue.exception.ResourceNotFoundException nếu không tìm thấy booking
     * @throws org.springframework.security.access.AccessDeniedException nếu booking không thuộc về customer
     * @throws com.sportvenue.exception.BadRequestException nếu booking không ở trạng thái cho phép
     */
    VnpayPaymentUrlResponse createVnpayPaymentUrl(UserPrincipal principal, Integer bookingId, String paymentOption, HttpServletRequest request);

    /**
     * Xử lý callback từ VNPay. Xác thực checksum, cập nhật trạng thái payment + booking.
     * Trả về {@link ReturnResult} chứa bookingId để controller redirect về frontend.
     *
     * @throws com.sportvenue.exception.BadRequestException nếu checksum không hợp lệ hoặc payment không tìm thấy
     */
    ReturnResult handleVnpayReturn(HttpServletRequest request);

    /**
     * Kết quả xử lý callback — controller dùng để redirect browser về VNPAY_RETURN_URL.
     */
    record ReturnResult(boolean success, Integer bookingId, String reason) {
    }

    /**
     * Xử lý callback IPN (Instant Payment Notification) — VNPay gọi server-to-server, độc lập với
     * việc khách có quay lại trình duyệt hay không. Dùng chung core logic với {@link #handleVnpayReturn}
     * qua {@code processVnpayCallback} (nội bộ) nhưng KHÔNG BAO GIỜ ném exception ra ngoài — luôn trả
     * về {@link VnpayIpnResponse} với {@code RspCode} đúng chuẩn VNPay để tránh bị retry vô hạn.
     */
    VnpayIpnResponse handleVnpayIpn(HttpServletRequest request);

    /**
     * Core xử lý dùng chung cho cả {@link #handleVnpayReturn} và {@link #handleVnpayIpn}: xác thực
     * checksum → xác thực {@code vnp_Amount} khớp với payment gốc → lookup payment có Pessimistic
     * Write Lock ({@code findByTransactionCodeForUpdate}, tránh race giữa 2 nguồn callback) → cập
     * nhật trạng thái Payment/Booking nếu là lần xử lý đầu tiên. Không ném exception cho các lỗi
     * nghiệp vụ dự kiến (checksum sai, order not found, amount sai) — trả về {@link CallbackOutcome}
     * để caller tự quyết định cách phản hồi.
     */
    CallbackOutcome processVnpayCallback(HttpServletRequest request);

    /**
     * Trạng thái xử lý 1 callback VNPay (return hoặc IPN) — đủ chi tiết để mỗi caller tự map sang
     * định dạng response riêng (redirect cho return, JSON RspCode cho IPN).
     */
    enum CallbackStatus {
        SUCCESS, ALREADY_CONFIRMED, ORDER_NOT_FOUND, INVALID_AMOUNT, INVALID_SIGNATURE, PAYMENT_FAILED, UNKNOWN_ERROR
    }

    /**
     * Kết quả xử lý core của 1 callback VNPay.
     * @param detail thông tin bổ sung tùy status — vd lý do lỗi cho UNKNOWN_ERROR/PAYMENT_FAILED,
     *               hoặc txnRef cho ORDER_NOT_FOUND (để caller tự dựng message phù hợp).
     */
    record CallbackOutcome(CallbackStatus status, Integer bookingId, String detail) {
    }

    /**
     * Thực hiện gọi API hoàn tiền sang cổng thanh toán (VNPay/MoMo).
     * @param originalPayment Giao dịch gốc
     * @param refundAmount Số tiền cần hoàn
     * @param refundReason Lý do hoàn tiền
     * @throws com.sportvenue.exception.PaymentGatewayRefundException nếu API cổng thanh toán trả về lỗi
     */
    void processRefund(com.sportvenue.entity.Payment originalPayment, java.math.BigDecimal refundAmount, String refundReason);

    /**
     * Truy vấn trạng thái giao dịch hoàn tiền từ cổng thanh toán (VNPay/MoMo) - QueryDR.
     * @param refundPayment Giao dịch hoàn tiền đang ở trạng thái PENDING
     * @return true nếu thành công, false nếu thất bại (hoặc ném exception nếu vẫn đang chờ).
     */
    boolean checkRefundStatus(com.sportvenue.entity.Payment refundPayment);
}
