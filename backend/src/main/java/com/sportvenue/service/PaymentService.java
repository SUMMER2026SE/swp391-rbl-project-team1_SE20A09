package com.sportvenue.service;

import com.sportvenue.dto.response.VnpayPaymentUrlResponse;
import com.sportvenue.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

/**
 * UC-CUS-02: Thanh toán đơn đặt sân qua VNPay.
 * <ul>
 *   <li>{@link #createVnpayPaymentUrl} — sinh paymentUrl cho customer</li>
 *   <li>{@link #handleVnpayReturn} — xử lý callback từ VNPay (public, không auth)</li>
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
