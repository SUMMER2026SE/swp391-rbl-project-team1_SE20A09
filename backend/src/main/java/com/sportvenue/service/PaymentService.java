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
}
