package com.sportvenue.service.impl;

import com.sportvenue.config.VNPayConfig;
import com.sportvenue.dto.response.VnpayPaymentUrlResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.PaymentService;
import com.sportvenue.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Triển khai VNPay payment cho UC-CUS-02.
 * <p>
 * Quy trình:
 * <ol>
 *   <li>Customer gọi {@link #createVnpayPaymentUrl} → sinh URL đã ký HMAC-SHA512</li>
 *   <li>Browser redirect sang VNPay sandbox → người dùng nhập thẻ</li>
 *   <li>VNPay redirect về {@code /api/v1/payments/vnpay-return} → {@link #handleVnpayReturn}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final int EXPIRE_MINUTES = 15;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public VnpayPaymentUrlResponse createVnpayPaymentUrl(UserPrincipal principal, Integer bookingId) {
        if (principal == null || principal.getUser() == null || principal.getUser().getUserId() == null) {
            throw new AccessDeniedException("Không xác định được người dùng");
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đặt sân ID: " + bookingId));

        // 1. Kiểm tra quyền sở hữu — chỉ chủ booking mới được thanh toán
        if (booking.getUser() == null
                || !booking.getUser().getUserId().equals(principal.getUser().getUserId())) {
            log.warn("User {} cố gắng thanh toán đơn {} của user {}",
                    principal.getUser().getUserId(), bookingId,
                    booking.getUser() != null ? booking.getUser().getUserId() : null);
            throw new AccessDeniedException("Bạn không có quyền thanh toán đơn đặt sân này");
        }

        // 2. Trạng thái booking phải PENDING
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Đơn này không thể thanh toán (trạng thái hiện tại: "
                    + booking.getBookingStatus() + ")");
        }

        // 3. Tránh double-pay — nếu PaymentStatus đã PAID thì chặn
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Đơn này đã được thanh toán");
        }

        // 4. Sinh mã giao dịch — dùng làm vnp_TxnRef và làm transactionCode để tra cứu khi return
        String txnRef = bookingId + "_" + System.currentTimeMillis();

        // 5. VNPay yêu cầu amount * 100 (đơn vị nhỏ nhất)
        BigDecimal totalPrice = booking.getTotalPrice();
        if (totalPrice == null) {
            throw new BadRequestException("Đơn đặt sân chưa có tổng tiền — không thể thanh toán");
        }
        long amountVnp = totalPrice.multiply(BigDecimal.valueOf(100)).longValueExact();

        // 6. Sinh createDate / expireDate theo múi giờ VN
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone(VN_ZONE));
        Date now = new Date();
        Date expire = new Date(now.getTime() + EXPIRE_MINUTES * 60_000L);
        String createDate = fmt.format(now);
        String expireDate = fmt.format(expire);

        // 7. Build tham số VNPay — alphabet order tự động áp dụng trong buildSignedQuery
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountVnp));
        params.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don dat san #" + bookingId);
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", resolveClientIp());
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // 8. Ký và ghép URL
        String signed = VNPayUtil.buildSignedQuery(params, vnPayConfig.getHashSecret());
        String paymentUrl = vnPayConfig.getUrl() + signed;

        // 9. Lưu Payment row — transactionCode = txnRef để tra cứu khi return
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(totalPrice)
                .transactionCode(txnRef)
                .paymentStatus(TransactionStatus.PENDING)
                .paidAt(null)
                .build();
        paymentRepository.save(payment);

        log.info("Đã tạo paymentUrl VNPay cho booking {} — txnRef={}, amount={}",
                bookingId, txnRef, totalPrice);

        return VnpayPaymentUrlResponse.builder()
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional
    public ReturnResult handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = VNPayUtil.extractVnpParams(request.getParameterMap());

        if (vnpParams.isEmpty()) {
            throw new BadRequestException("Không nhận được tham số VNPay");
        }

        // 1. Xác thực checksum — KHÔNG dùng stub, kiểm tra thật
        if (!VNPayUtil.verifyChecksum(vnpParams, vnPayConfig.getHashSecret())) {
            log.warn("VNPay return có checksum không hợp lệ — txnRef={}",
                    vnpParams.get("vnp_TxnRef"));
            Integer fallbackBookingId = VNPayUtil.extractBookingIdFromOrderInfo(
                    vnpParams.get("vnp_OrderInfo"));
            return new ReturnResult(false, fallbackBookingId, "invalid_hash");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new BadRequestException("Thiếu vnp_TxnRef trong callback");
        }

        Payment payment = paymentRepository.findByTransactionCode(txnRef)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy payment với txnRef: " + txnRef));

        // Chỉ xử lý lần đầu — nếu đã SUCCESS thì idempotent trả về success luôn
        if (payment.getPaymentStatus() == TransactionStatus.SUCCESS) {
            Booking alreadyPaid = payment.getBooking();
            return new ReturnResult(true,
                    alreadyPaid != null ? alreadyPaid.getBookingId() : null,
                    null);
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        Booking booking = payment.getBooking();
        boolean success = "00".equals(responseCode);

        if (success) {
            payment.setPaymentStatus(TransactionStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            if (booking != null) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                booking.setPaymentStatus(PaymentStatus.PAID);
                bookingRepository.save(booking);
            }
            log.info("VNPay thanh toán THÀNH CÔNG — txnRef={}, booking={}",
                    txnRef, booking != null ? booking.getBookingId() : null);
        } else {
            payment.setPaymentStatus(TransactionStatus.FAILED);
            if (booking != null) {
                // Giữ nguyên bookingStatus = PENDING — không confirm khi chưa trả tiền
                bookingRepository.save(booking);
            }
            log.warn("VNPay thanh toán THẤT BẠI — txnRef={}, responseCode={}",
                    txnRef, responseCode);
        }
        paymentRepository.save(payment);

        Integer bookingId = booking != null ? booking.getBookingId() : null;
        return new ReturnResult(success, bookingId, success ? null : ("response_code=" + responseCode));
    }

    /**
     * Lấy IP client — thử X-Forwarded-For trước (khi qua proxy), fallback sang remoteAddr.
     * Giá trị gửi cho VNPay không bắt buộc chính xác trong sandbox nhưng vẫn cần có.
     */
    private String resolveClientIp() {
        try {
            // Cách tiếp cận đơn giản: dùng InetAddress.localhost — service không có request ở đây
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }
}
