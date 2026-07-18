package com.sportvenue.service.impl;

import com.sportvenue.config.VNPayConfig;
import com.sportvenue.dto.response.VnpayIpnResponse;
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
import com.sportvenue.service.CustomerNotificationService;
import com.sportvenue.service.PaymentService;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.VnpayRefundGateway;
import com.sportvenue.util.AfterCommitExecutor;
import com.sportvenue.util.VNPayUtil;
import com.sportvenue.entity.Owner;
import com.sportvenue.service.WalletService;
import com.sportvenue.entity.enums.WalletTransactionType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
    private final com.sportvenue.service.AdminDashboardService adminDashboardService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final CustomerNotificationService customerNotificationService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final VnpayRefundGateway vnpayRefundGateway;
    private final WalletService walletService;

    @Override
    @Transactional
    public VnpayPaymentUrlResponse createVnpayPaymentUrl(UserPrincipal principal, Integer bookingId, String paymentOption, HttpServletRequest request) {
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

        // 2. Trạng thái booking phải PENDING_PAYMENT — sau khi tạo đơn (UC-CUS-01) booking
        // ở PENDING_PAYMENT, chờ khách thanh toán qua VNPay. Sau callback thành công sẽ
        // chuyển sang CONFIRMED ở handleVnpayReturn().
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Đơn này không thể thanh toán (trạng thái hiện tại: "
                    + booking.getBookingStatus() + ")");
        }

        // 2a. Kiểm tra expiredAt — nếu quá hạn 5 phút thì không cho thanh toán
        if (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Đơn đặt sân đã hết hạn giữ chỗ — vui lòng đặt lại");
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
        
        BigDecimal amountToPay = totalPrice;
        if ("DEPOSIT".equalsIgnoreCase(paymentOption)) {
            amountToPay = totalPrice.multiply(new BigDecimal("0.3")).setScale(0, java.math.RoundingMode.CEILING);
        }
        
        long amountVnp = amountToPay.multiply(BigDecimal.valueOf(100)).longValueExact();

        // 6. Sinh createDate / expireDate theo múi giờ VN
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone(VN_ZONE));
        Date now = new Date();
        Date expire = new Date(now.getTime() + EXPIRE_MINUTES * 60_000L);
        String createDate = fmt.format(now);
        String expireDate = fmt.format(expire);

        // 7. Build tham số VNPay — alphabet order tự động áp dụng trong buildSignedQuery
        Map<String, String> params = buildVnpParams(txnRef, amountVnp, createDate, expireDate, bookingId, request);

        // 8. Ký và ghép URL
        String signed = VNPayUtil.buildSignedQuery(params, vnPayConfig.getHashSecret());
        String paymentUrl = vnPayConfig.getUrl() + signed;

        // 9. Lưu Payment row — transactionCode = txnRef để tra cứu khi return
        savePayment(booking, amountToPay, txnRef);

        log.info("Đã tạo paymentUrl VNPay cho booking {} — txnRef={}, amount={}",
                bookingId, txnRef, amountToPay);

        return VnpayPaymentUrlResponse.builder()
                .paymentUrl(paymentUrl)
                .build();
    }

    private Map<String, String> buildVnpParams(String txnRef, long amountVnp, String createDate,
                                               String expireDate, Integer bookingId, HttpServletRequest request) {
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
        params.put("vnp_IpAddr", resolveClientIp(request));
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);
        return params;
    }

    private void savePayment(Booking booking, BigDecimal totalPrice, String txnRef) {
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(totalPrice)
                .transactionCode(txnRef)
                .paymentStatus(TransactionStatus.PENDING)
                .paidAt(null)
                .build();
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public ReturnResult handleVnpayReturn(HttpServletRequest request) {
        CallbackOutcome outcome = processVnpayCallback(request);
        return switch (outcome.status()) {
            case SUCCESS, ALREADY_CONFIRMED -> new ReturnResult(true, outcome.bookingId(), null);
            case PAYMENT_FAILED -> new ReturnResult(false, outcome.bookingId(), outcome.detail());
            case INVALID_SIGNATURE -> new ReturnResult(false, outcome.bookingId(), "invalid_hash");
            case INVALID_AMOUNT -> new ReturnResult(false, outcome.bookingId(), "invalid_amount");
            case ORDER_NOT_FOUND -> throw new BadRequestException(
                    "Không tìm thấy payment với txnRef: " + outcome.detail());
            case UNKNOWN_ERROR -> throw new BadRequestException(outcome.detail());
        };
    }

    @Override
    @Transactional
    public VnpayIpnResponse handleVnpayIpn(HttpServletRequest request) {
        CallbackOutcome outcome = processVnpayCallback(request);
        return switch (outcome.status()) {
            case SUCCESS, PAYMENT_FAILED ->
                    VnpayIpnResponse.builder().rspCode("00").message("Confirm Success").build();
            case ALREADY_CONFIRMED ->
                    VnpayIpnResponse.builder().rspCode("02").message("Order already confirmed").build();
            case ORDER_NOT_FOUND ->
                    VnpayIpnResponse.builder().rspCode("01").message("Order not found").build();
            case INVALID_AMOUNT ->
                    VnpayIpnResponse.builder().rspCode("04").message("Invalid amount").build();
            case INVALID_SIGNATURE ->
                    VnpayIpnResponse.builder().rspCode("97").message("Invalid signature").build();
            case UNKNOWN_ERROR ->
                    VnpayIpnResponse.builder().rspCode("99").message("Unknown error").build();
        };
    }

    /**
     * Core xử lý dùng chung cho return (browser redirect) và IPN (server-to-server) — xem
     * {@link PaymentService#processVnpayCallback} cho mô tả từng bước. Không ném exception cho các
     * lỗi nghiệp vụ dự kiến; caller ({@link #handleVnpayReturn}/{@link #handleVnpayIpn}) tự map
     * {@link CallbackOutcome} sang định dạng response riêng.
     */
    @Override
    @Transactional
    public CallbackOutcome processVnpayCallback(HttpServletRequest request) {
        Map<String, String> vnpParams = VNPayUtil.extractVnpParams(request.getParameterMap());

        if (vnpParams.isEmpty()) {
            return new CallbackOutcome(CallbackStatus.UNKNOWN_ERROR, null, "Không nhận được tham số VNPay");
        }

        // 1. Xác thực checksum — KHÔNG dùng stub, kiểm tra thật
        if (!VNPayUtil.verifyChecksum(vnpParams, vnPayConfig.getHashSecret())) {
            log.warn("VNPay callback có checksum không hợp lệ — txnRef={}", vnpParams.get("vnp_TxnRef"));
            Integer fallbackBookingId = VNPayUtil.extractBookingIdFromOrderInfo(
                    vnpParams.get("vnp_OrderInfo"));
            return new CallbackOutcome(CallbackStatus.INVALID_SIGNATURE, fallbackBookingId, "invalid_hash");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return new CallbackOutcome(CallbackStatus.UNKNOWN_ERROR, null, "Thiếu vnp_TxnRef trong callback");
        }

        // 2. Lookup có Pessimistic Write Lock — tránh race giữa return-redirect và IPN cùng ghi 1 row
        Payment payment = paymentRepository.findByTransactionCodeForUpdate(txnRef).orElse(null);
        if (payment == null) {
            return new CallbackOutcome(CallbackStatus.ORDER_NOT_FOUND, null, txnRef);
        }

        Booking booking = payment.getBooking();
        Integer bookingId = booking != null ? booking.getBookingId() : null;

        // 3. Idempotent — đã được xử lý thành công trước đó (return hoặc IPN tới trước)
        if (payment.getPaymentStatus() == TransactionStatus.SUCCESS) {
            return new CallbackOutcome(CallbackStatus.ALREADY_CONFIRMED, bookingId, null);
        }

        // 4. Xác thực vnp_Amount khớp số tiền payment gốc — chặn callback giả mạo sửa số tiền
        long expectedAmount;
        try {
            expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        } catch (ArithmeticException ex) {
            expectedAmount = -1L;
        }
        long vnpAmount;
        try {
            vnpAmount = Long.parseLong(vnpParams.get("vnp_Amount"));
        } catch (NumberFormatException ex) {
            vnpAmount = -1L;
        }
        if (vnpAmount != expectedAmount) {
            log.warn("VNPay callback amount không khớp — txnRef={}, expected={}, got={}",
                    txnRef, expectedAmount, vnpAmount);
            return new CallbackOutcome(CallbackStatus.INVALID_AMOUNT, bookingId, "amount_mismatch");
        }

        // 5. Xử lý thành công/thất bại
        String responseCode = vnpParams.get("vnp_ResponseCode");
        boolean success = "00".equals(responseCode);

        if (success) {
            processSuccessfulPayment(payment, booking, txnRef, vnpParams);
        } else {
            processFailedPayment(payment, booking, txnRef, responseCode);
        }
        paymentRepository.save(payment);

        return new CallbackOutcome(
                success ? CallbackStatus.SUCCESS : CallbackStatus.PAYMENT_FAILED,
                bookingId,
                success ? null : ("response_code=" + responseCode));
    }

    private void processSuccessfulPayment(Payment payment, Booking booking, String txnRef,
                                          Map<String, String> vnpParams) {
        payment.setPaymentStatus(TransactionStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayTransactionNo(vnpParams.get("vnp_TransactionNo"));
        payment.setGatewayPayDate(vnpParams.get("vnp_PayDate"));
        if (booking != null) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            if (payment.getAmount().compareTo(booking.getTotalPrice()) >= 0) {
                booking.setPaymentStatus(PaymentStatus.PAID);
            } else {
                booking.setPaymentStatus(PaymentStatus.DEPOSITED);
            }
            bookingRepository.save(booking);

            // Ghi nhận vào ví nội bộ
            Owner resolvedOwner = booking.getStadium() != null ? booking.getStadium().resolveOwner() : null;
            if (resolvedOwner != null) {
                BigDecimal totalAmount = payment.getAmount();
                BigDecimal serviceFee = booking.getServiceFee() != null ? booking.getServiceFee() : BigDecimal.ZERO;
                if (booking.getPaymentStatus() == PaymentStatus.PAID) {
                    BigDecimal ownerShare = totalAmount.subtract(serviceFee);
                    walletService.recordOwnerTransaction(
                            resolvedOwner.getOwnerId(),
                            ownerShare,
                            booking.getBookingId(),
                            WalletTransactionType.BOOKING_CREDIT,
                            "Doanh thu đặt sân (đã trừ phí dịch vụ)"
                    );
                    if (serviceFee.compareTo(BigDecimal.ZERO) > 0) {
                        walletService.recordPlatformTransaction(
                                serviceFee,
                                booking.getBookingId(),
                                WalletTransactionType.SERVICE_FEE_CREDIT,
                                "Phí dịch vụ từ đơn đặt sân #" + booking.getBookingId()
                        );
                    }
                } else if (booking.getPaymentStatus() == PaymentStatus.DEPOSITED) {
                    // Trừ phí dịch vụ ngay lúc cọc (không đợi tới lúc thu nốt) — tiền cọc đã là
                    // giao dịch thật qua VNPay, nên Platform cần được ghi nhận doanh thu ngay,
                    // tránh trường hợp khách hủy trước khi thu nốt khiến Platform mất trắng phí.
                    BigDecimal ownerShare = totalAmount.subtract(serviceFee);
                    walletService.recordOwnerTransaction(
                            resolvedOwner.getOwnerId(),
                            ownerShare,
                            booking.getBookingId(),
                            WalletTransactionType.BOOKING_CREDIT,
                            "Tiền đặt cọc đặt sân #" + booking.getBookingId() + " (đã trừ phí dịch vụ)"
                    );
                    if (serviceFee.compareTo(BigDecimal.ZERO) > 0) {
                        walletService.recordPlatformTransaction(
                                serviceFee,
                                booking.getBookingId(),
                                WalletTransactionType.SERVICE_FEE_CREDIT,
                                "Phí dịch vụ từ đơn đặt cọc #" + booking.getBookingId()
                        );
                    }
                }
            }

            final Booking finalBookingForCallback = booking;
            try {
                customerNotificationService.notifyPaymentReceived(finalBookingForCallback.getUser().getUserId(), payment);
            } catch (Exception e) {
                log.error("Failed to publish payment notification for booking {}", finalBookingForCallback.getBookingId(), e);
            }
        }
        log.info("VNPay thanh toán THÀNH CÔNG — txnRef={}, booking={}",
                txnRef, booking != null ? booking.getBookingId() : null);
        // Xóa cache dashboard — doanh thu / booking confirmed thay đổi
        adminDashboardService.evictDashboardCache();
    }

    private void processFailedPayment(Payment payment, Booking booking, String txnRef, String responseCode) {
        payment.setPaymentStatus(TransactionStatus.FAILED);
        if (booking != null) {
            // Giữ nguyên bookingStatus = PENDING — không confirm khi chưa trả tiền
            bookingRepository.save(booking);

            final Booking finalBookingForCallback = booking;
            try {
                customerNotificationService.notifyPaymentFailed(finalBookingForCallback.getUser().getUserId(), payment);
            } catch (Exception e) {
                log.error("Failed to publish payment failed notification for booking {}", finalBookingForCallback.getBookingId(), e);
            }
        }
        log.warn("VNPay thanh toán THẤT BẠI — txnRef={}, responseCode={}",
                txnRef, responseCode);
    }

    /**
     * Lấy IP client — thử X-Forwarded-For trước (khi qua proxy), fallback sang remoteAddr.
     * Giá trị gửi cho VNPay không bắt buộc chính xác trong sandbox nhưng vẫn cần có.
     */
    private String resolveClientIp(HttpServletRequest request) {
        try {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    @Override
    public void processRefund(Payment originalPayment, BigDecimal refundAmount, String refundReason) {
        vnpayRefundGateway.refund(originalPayment, refundAmount, refundReason);
    }

    @Override
    public boolean checkRefundStatus(Payment refundPayment) {
        return vnpayRefundGateway.queryRefundStatus(refundPayment);
    }
}
