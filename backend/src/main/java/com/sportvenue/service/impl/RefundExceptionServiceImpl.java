package com.sportvenue.service.impl;

import com.sportvenue.dto.request.AdminDecisionRequest;
import com.sportvenue.dto.request.OwnerReviewRequest;
import com.sportvenue.dto.request.SubmitExceptionRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.RefundExceptionResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.RefundExceptionStatus;
import com.sportvenue.entity.enums.RefundReasonType;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.RefundExceptionRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.CustomerNotificationService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.PaymentService;
import com.sportvenue.service.RefundExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundExceptionServiceImpl implements RefundExceptionService {

    private static final int SUBMIT_DEADLINE_HOURS = 72;
    private static final List<RefundExceptionStatus> ACTIVE_STATUSES = List.of(
            RefundExceptionStatus.PENDING_OWNER,
            RefundExceptionStatus.PENDING_ADMIN,
            RefundExceptionStatus.APPROVED_OWNER,
            RefundExceptionStatus.APPROVED_ADMIN
    );

    private final RefundExceptionRepository refundExceptionRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CustomerNotificationService customerNotificationService;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;

    // ─────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ─────────────────────────────────────────────────────────

    @Override
    public RefundExceptionResponse submitRequest(String customerEmail, SubmitExceptionRequest req) {
        log.info("[RefundException] Customer {} submitting exception for bookingId={}", customerEmail, req.getBookingId());

        User customer = findUserByEmail(customerEmail);
        Booking booking = findBookingById(req.getBookingId());

        validateSubmitRequest(customer, booking, req);

        RefundExceptionRequest request = RefundExceptionRequest.builder()
                .booking(booking)
                .customer(customer)
                .reason(req.getReason())
                .evidenceUrl(req.getEvidenceUrl())
                .status(RefundExceptionStatus.PENDING_OWNER)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(SUBMIT_DEADLINE_HOURS))
                .build();

        RefundExceptionRequest saved = refundExceptionRepository.save(request);

        // Notify Owner
        Integer ownerUserId = booking.getStadium().getOwner().getUser().getUserId();
        notificationService.publishNotificationEvent(
                ownerUserId,
                "Yêu cầu ngoại lệ hoàn tiền mới",
                "Khách " + customer.getFullName() + " gửi yêu cầu xem xét ngoại lệ cho đơn #" + booking.getBookingId(),
                NotificationType.BOOKING,
                String.valueOf(saved.getRequestId())
        );

        log.info("[RefundException] Created requestId={} for bookingId={}", saved.getRequestId(), req.getBookingId());
        return toResponse(saved);
    }

    private void validateSubmitRequest(User customer, Booking booking, SubmitExceptionRequest req) {
        // 1. Booking phải thuộc về khách đang gọi
        if (!booking.getUser().getUserId().equals(customer.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền gửi yêu cầu cho đơn này.");
        }
        // 2. Booking phải ở trạng thái CANCELLED
        if (booking.getBookingStatus() != BookingStatus.CANCELLED) {
            throw new BadRequestException("Chỉ có thể gửi yêu cầu ngoại lệ cho đơn đã hủy.");
        }
        // 3. Lấy refund Payment gốc (nếu có)
        Optional<Payment> existingRefund = paymentRepository.findRefundPaymentByBookingId(req.getBookingId())
                .stream().findFirst();
        LocalDateTime cancelledAt = existingRefund.map(Payment::getPaidAt).orElse(booking.getBookingDate());
        if (cancelledAt != null && cancelledAt.isBefore(LocalDateTime.now().minusHours(SUBMIT_DEADLINE_HOURS))) {
            throw new BadRequestException("Đã quá " + SUBMIT_DEADLINE_HOURS + "h kể từ khi hủy đơn — không thể gửi yêu cầu.");
        }
        // 4. Chưa có request đang xử lý
        if (refundExceptionRepository.existsByBookingBookingIdAndStatusIn(req.getBookingId(), ACTIVE_STATUSES)) {
            throw new BadRequestException("Đơn này đã có yêu cầu ngoại lệ đang được xử lý.");
        }
        // 5. Phải từng có 1 giao dịch thanh toán THẬT qua cổng thanh toán
        Payment originalPayment = paymentRepository.findSuccessPaymentsByBookingId(req.getBookingId())
                .stream().findFirst().orElse(null);
        if (originalPayment == null) {
            throw new BadRequestException("Đơn này chưa từng thanh toán qua cổng thanh toán — không có gì để hoàn.");
        }
        // 5.5 Chặn đơn đặt cọc — Chỉ áp dụng yêu cầu ngoại lệ cho đơn thanh toán toàn bộ (100%)
        BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal serviceFee = booking.getServiceFee() != null ? booking.getServiceFee() : BigDecimal.ZERO;
        BigDecimal baseCourtPrice = totalPrice.subtract(serviceFee);
        if (originalPayment.getAmount().compareTo(baseCourtPrice) < 0) {
            throw new BadRequestException("Yêu cầu ngoại lệ không áp dụng cho đơn đặt cọc (chỉ áp dụng cho đơn thanh toán toàn bộ).");
        }
        // 6. [Guard] Chỉ áp dụng cho booking đã hoàn 0% hoặc một phần
        existingRefund.ifPresent(refund -> {
            if (refund.getPaymentStatus() != TransactionStatus.FAILED
                    && refund.getAmount().abs().compareTo(originalPayment.getAmount()) >= 0) {
                throw new BadRequestException("Booking đã được hoàn tiền toàn bộ — không đủ điều kiện xin ngoại lệ.");
            }
        });
    }

    @Override
    public RefundExceptionResponse ownerReview(String ownerEmail, Integer requestId, OwnerReviewRequest req) {
        log.info("[RefundException] Owner {} reviewing requestId={}, approved={}", ownerEmail, requestId, req.isApproved());

        RefundExceptionRequest request = findRequestById(requestId);

        // Validate Owner sở hữu sân
        String stadiumOwnerEmail = request.getBooking().getStadium().getOwner().getUser().getEmail();
        if (!stadiumOwnerEmail.equals(ownerEmail)) {
            throw new ForbiddenException("Bạn không có quyền duyệt yêu cầu cho sân này.");
        }
        // Validate trạng thái
        if (request.getStatus() != RefundExceptionStatus.PENDING_OWNER) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ duyệt Owner (status=" + request.getStatus() + ").");
        }
        // Validate business rule approved/refundPercent
        validateApprovedRefundPercent(req.isApproved(), req.getRefundPercent());

        request.setOwnerNote(req.getOwnerNote());
        request.setOwnerReviewedAt(LocalDateTime.now());

        if (req.isApproved()) {
            request.setStatus(RefundExceptionStatus.APPROVED_OWNER);
            request.setRefundPercent(req.getRefundPercent());
            RefundExceptionRequest saved = refundExceptionRepository.save(request);

            // Notify customer
            notifyCustomer(request, "Yêu cầu ngoại lệ được chấp nhận",
                    "Owner đã chấp nhận hoàn " + req.getRefundPercent() + "% cho đơn #" + request.getBooking().getBookingId() + ".");
            try {
                customerNotificationService.notifyRefundExceptionDecision(request.getCustomer().getUserId(), request, true);
            } catch (Exception ex) {
                log.warn("Failed to emit refund exception decision notification for request {}", request.getRequestId(), ex);
            }

            // Phase 1 → Phase 2 (tuần tự, cùng method — không afterCommit, không REQUIRES_NEW)
            Payment pendingRefund = createPendingRefundRecord(saved);
            executeGatewayRefund(saved, pendingRefund);

        } else {
            request.setStatus(RefundExceptionStatus.REJECTED_OWNER);
            refundExceptionRepository.save(request);

            notifyCustomer(request, "Yêu cầu ngoại lệ bị từ chối bởi Owner",
                    "Owner đã từ chối. Bạn có thể leo thang lên Admin trong 72h kể từ khi tạo yêu cầu.");
            try {
                customerNotificationService.notifyRefundExceptionDecision(request.getCustomer().getUserId(), request, false);
            } catch (Exception ex) {
                log.warn("Failed to emit refund exception decision notification for request {}", request.getRequestId(), ex);
            }
        }

        return toResponse(request);
    }

    @Override
    public RefundExceptionResponse customerEscalate(String customerEmail, Integer requestId) {
        log.info("[RefundException] Customer {} escalating requestId={}", customerEmail, requestId);

        RefundExceptionRequest request = findRequestById(requestId);

        if (!request.getCustomer().getEmail().equals(customerEmail)) {
            throw new ForbiddenException("Bạn không có quyền leo thang yêu cầu này.");
        }
        // Cho leo thang cả khi Owner đã CHẤP NHẬN (thường là 50%) nhưng khách chưa hài lòng,
        // không chỉ khi bị từ chối hẳn — Admin xem xét lại, có thể duyệt thêm phần chênh lệch
        // (xem createPendingRefundRecord: chỉ hoàn phần còn thiếu, không hoàn lại từ đầu).
        if (request.getStatus() != RefundExceptionStatus.REJECTED_OWNER
                && request.getStatus() != RefundExceptionStatus.APPROVED_OWNER) {
            throw new BadRequestException("Chỉ có thể leo thang khi Owner đã từ chối hoặc đã chấp nhận một phần (status hiện tại=" + request.getStatus() + ").");
        }
        if (LocalDateTime.now().isAfter(request.getExpiresAt())) {
            throw new BadRequestException("Đã quá thời hạn 72h — không thể leo thang Admin.");
        }

        boolean wasApproved = request.getStatus() == RefundExceptionStatus.APPROVED_OWNER;
        request.setStatus(RefundExceptionStatus.PENDING_ADMIN);
        refundExceptionRepository.save(request);

        // Notify Admin (tất cả admin)
        String escalateContext = wasApproved
                ? " sau khi không hài lòng với mức Owner đã chấp nhận (" + request.getRefundPercent() + "%)."
                : " sau khi Owner từ chối.";
        userRepository.findAllAdmins().forEach(admin ->
                notificationService.publishNotificationEvent(
                        admin.getUserId(),
                        "Yêu cầu ngoại lệ leo thang lên Admin",
                        "Khách " + request.getCustomer().getFullName()
                                + " leo thang yêu cầu #" + requestId + escalateContext,
                        NotificationType.BOOKING,
                        String.valueOf(requestId)
                )
        );

        log.info("[RefundException] Escalated requestId={} to PENDING_ADMIN", requestId);
        return toResponse(request);
    }

    @Override
    public RefundExceptionResponse adminDecide(String adminEmail, Integer requestId, AdminDecisionRequest req) {
        log.info("[RefundException] Admin {} deciding requestId={}, approved={}", adminEmail, requestId, req.isApproved());

        RefundExceptionRequest request = findRequestById(requestId);

        if (request.getStatus() != RefundExceptionStatus.PENDING_ADMIN) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ Admin (status=" + request.getStatus() + ").");
        }
        validateApprovedRefundPercent(req.isApproved(), req.getRefundPercent());

        request.setAdminNote(req.getAdminNote());
        request.setAdminReviewedAt(LocalDateTime.now());

        if (req.isApproved()) {
            request.setStatus(RefundExceptionStatus.APPROVED_ADMIN);
            request.setRefundPercent(req.getRefundPercent());
            RefundExceptionRequest saved = refundExceptionRepository.save(request);

            notifyCustomer(request, "Yêu cầu ngoại lệ được Admin chấp nhận",
                    "Admin đã chấp nhận hoàn " + req.getRefundPercent() + "% cho đơn #" + request.getBooking().getBookingId() + ".");
            try {
                customerNotificationService.notifyRefundExceptionDecision(request.getCustomer().getUserId(), request, true);
            } catch (Exception ex) {
                log.warn("Failed to emit refund exception decision notification for request {}", request.getRequestId(), ex);
            }

            // Phase 1 → Phase 2 (tuần tự)
            Payment pendingRefund = createPendingRefundRecord(saved);
            executeGatewayRefund(saved, pendingRefund);

        } else {
            request.setStatus(RefundExceptionStatus.REJECTED_ADMIN);
            refundExceptionRepository.save(request);

            notifyCustomer(request, "Yêu cầu ngoại lệ bị Admin từ chối",
                    "Admin đã xem xét và từ chối yêu cầu ngoại lệ cho đơn #" + request.getBooking().getBookingId() + ".");
            try {
                customerNotificationService.notifyRefundExceptionDecision(request.getCustomer().getUserId(), request, false);
            } catch (Exception ex) {
                log.warn("Failed to emit refund exception decision notification for request {}", request.getRequestId(), ex);
            }
        }

        return toResponse(request);
    }

    @Override
    public PageResponse<RefundExceptionResponse> getCustomerRequests(String customerEmail, Pageable pageable) {
        User customer = findUserByEmail(customerEmail);
        Page<RefundExceptionRequest> page = refundExceptionRepository
                .findByCustomerUserIdOrderByCreatedAtDesc(customer.getUserId(), pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<RefundExceptionResponse> getOwnerPendingRequests(String ownerEmail, Pageable pageable) {
        List<RefundExceptionStatus> ownerStatuses = List.of(
                RefundExceptionStatus.PENDING_OWNER,
                RefundExceptionStatus.APPROVED_OWNER,
                RefundExceptionStatus.REJECTED_OWNER
        );
        Page<RefundExceptionRequest> page = refundExceptionRepository
                .findByOwnerEmailAndStatusIn(ownerEmail, ownerStatuses, pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<RefundExceptionResponse> getAdminQueue(Pageable pageable) {
        Page<RefundExceptionRequest> page = refundExceptionRepository
                .findAllByStatusIn(List.of(RefundExceptionStatus.PENDING_ADMIN), pageable);
        return toPageResponse(page);
    }

    @Override
    public RefundExceptionResponse getLatestByBookingId(Integer bookingId) {
        return refundExceptionRepository.findLatestByBookingId(bookingId)
                .stream().findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Phase 1: Ghi sổ Payment PENDING trong transaction riêng (commit trước gateway).
     *
     * <p>Dùng {@code transactionTemplate.execute()} tường minh thay vì {@code @Transactional} —
     * method này luôn được gọi bằng {@code this.createPendingRefundRecord(...)} từ trong cùng
     * class ({@code ownerReview}/{@code adminDecide}), là self-invocation nên proxy AOP của
     * {@code @Transactional} sẽ bị Spring bỏ qua hoàn toàn (annotation vô tác dụng, dễ gây hiểu
     * lầm và tiềm ẩn rủi ro nếu sau này {@code ownerReview}/{@code adminDecide} được thêm
     * {@code @Transactional} bao ngoài — lúc đó gateway call trong Phase 2 sẽ vô tình chạy trong
     * lúc transaction vẫn mở). Đồng nhất với cách {@code executeGatewayRefund()} bên dưới làm.</p>
     */
    private Payment createPendingRefundRecord(RefundExceptionRequest request) {
        Integer bookingId = request.getBooking().getBookingId();
        Payment originalPayment = paymentRepository
                .findSuccessPaymentsByBookingId(bookingId)
                .stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch gốc cho booking #" + bookingId));

        BigDecimal serviceFee = request.getBooking().getServiceFee() != null ? request.getBooking().getServiceFee() : BigDecimal.ZERO;
        BigDecimal baseAmount = originalPayment.getAmount().subtract(serviceFee);
        if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            baseAmount = BigDecimal.ZERO;
        }

        BigDecimal desiredAmount = baseAmount
                .multiply(BigDecimal.valueOf(request.getRefundPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        // Nếu request này từng được Owner duyệt 1 phần rồi khách leo thang lên Admin duyệt cao
        // hơn (vd Owner 50% -> Admin 100%), chỉ hoàn phần CHÊNH LỆCH — không tính lại từ đầu để
        // tránh trả tiền 2 lần cho cùng 1 khoản đã chi trước đó.
        BigDecimal alreadyRefunded = paymentRepository.findRefundPaymentByBookingId(bookingId).stream()
                .filter(p -> p.getPaymentStatus() == TransactionStatus.SUCCESS)
                .map(p -> p.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundAmount = desiredAmount.subtract(alreadyRefunded);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        String transactionCode = alreadyRefunded.compareTo(BigDecimal.ZERO) > 0
                ? "EXC_RFND_TOPUP_" + request.getRequestId() + "_" + originalPayment.getTransactionCode()
                : "EXC_RFND_" + originalPayment.getTransactionCode();

        final BigDecimal finalRefundAmount = refundAmount;
        Payment saved = transactionTemplate.execute(status -> {
            Payment refundPayment = Payment.builder()
                    .booking(request.getBooking())
                    .paymentMethod(originalPayment.getPaymentMethod())
                    .amount(finalRefundAmount.negate())
                    .transactionCode(transactionCode)
                    .paymentStatus(TransactionStatus.PENDING)
                    .paidAt(LocalDateTime.now())
                    .reasonType(RefundReasonType.FORCE_MAJEURE)
                    .build();
            return paymentRepository.save(refundPayment);
        });

        log.info("[RefundException] Phase1: Created PENDING refund paymentId={} amount={} (đã hoàn trước đó={}) for requestId={}",
                saved.getPaymentId(), finalRefundAmount, alreadyRefunded, request.getRequestId());
        return saved;
    }

    /**
     * Phase 2: Gọi gateway và cập nhật trạng thái Payment trong transaction riêng.
     * Không có @Transactional bao ngoài — exception propagate về controller → client nhận 400.
     * Gateway lỗi: Payment=FAILED, request status giữ nguyên APPROVED_* → Admin có thể audit/retry.
     *
     * <p>Chính sách phí dịch vụ FORCE_MAJEURE: hoàn refundPercent × (originalPayment.getAmount() - serviceFee)
     * (KHÔNG bao gồm phí dịch vụ, để giữ tính công bằng tài chính với Owner và đồng nhất với khách tự hủy standard).</p>
     */
    private void executeGatewayRefund(RefundExceptionRequest request, Payment pendingRefundPayment) {
        Payment originalPayment = paymentRepository
                .findSuccessPaymentsByBookingId(request.getBooking().getBookingId())
                .stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch gốc."));

        BigDecimal refundAmount = pendingRefundPayment.getAmount().abs();

        boolean gatewaySuccess;
        if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
            // Top-up = 0đ (vd Admin duyệt đúng bằng % Owner đã duyệt trước) -> không có gì thêm
            // để hoàn, không gọi gateway (gateway thật/mock đều từ chối yêu cầu hoàn 0đ).
            gatewaySuccess = true;
        } else {
            gatewaySuccess = false;
            try {
                paymentService.processRefund(originalPayment, refundAmount,
                        "Ngoại lệ bất khả kháng — Yêu cầu #" + request.getRequestId());
                gatewaySuccess = true;
            } catch (Exception e) {
                log.error("[RefundException] Gateway failed for requestId={}, bookingId={}",
                        request.getRequestId(), request.getBooking().getBookingId(), e);
            }
        }

        final boolean success = gatewaySuccess;
        final Integer bookingId = request.getBooking().getBookingId();
        final Integer paymentId = pendingRefundPayment.getPaymentId();

        transactionTemplate.execute(status -> {
            Payment saved = paymentRepository.findById(paymentId).orElse(null);
            if (saved != null) {
                saved.setPaymentStatus(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
                paymentRepository.save(saved);
            }
            if (success) {
                Booking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking != null) {
                    booking.setPaymentStatus(PaymentStatus.REFUNDED);
                    bookingRepository.save(booking);
                }
            }
            return null;
        });

        if (!gatewaySuccess) {
            throw new BadRequestException(
                    "Yêu cầu đã được duyệt nhưng hoàn tiền thực tế thất bại do lỗi cổng thanh toán. "
                    + "Admin vui lòng kiểm tra Payment #" + paymentId + " và retry thủ công.");
        }

        log.info("[RefundException] Phase2: Gateway refund SUCCESS for requestId={}, amount={}",
                request.getRequestId(), refundAmount);
    }

    /** Validate business rule: approved=true → refundPercent ∈ {50,100}; false → null. */
    private void validateApprovedRefundPercent(boolean approved, Integer refundPercent) {
        if (approved) {
            if (refundPercent == null || (refundPercent != 50 && refundPercent != 100)) {
                throw new BadRequestException("Khi chấp nhận (approved=true), refundPercent phải là 50 hoặc 100.");
            }
        } else {
            if (refundPercent != null) {
                throw new BadRequestException("Khi từ chối (approved=false), refundPercent phải để trống (null).");
            }
        }
    }

    private void notifyCustomer(RefundExceptionRequest request, String title, String message) {
        try {
            notificationService.publishNotificationEvent(
                    request.getCustomer().getUserId(),
                    title, message,
                    NotificationType.BOOKING,
                    String.valueOf(request.getRequestId())
            );
        } catch (Exception e) {
            log.warn("[RefundException] Failed to notify customer for requestId={}", request.getRequestId(), e);
        }
    }

    private RefundExceptionResponse toResponse(RefundExceptionRequest r) {
        boolean expired = LocalDateTime.now().isAfter(r.getExpiresAt());
        boolean canEscalate = (r.getStatus() == RefundExceptionStatus.REJECTED_OWNER
                || (r.getStatus() == RefundExceptionStatus.APPROVED_OWNER && r.getRefundPercent() != null && r.getRefundPercent() < 100)) && !expired;

        String reservationDate = r.getBooking().getReservationDate() != null
                ? r.getBooking().getReservationDate().toString() : null;
        String stadiumName = r.getBooking().getStadium() != null
                ? r.getBooking().getStadium().getStadiumName() : null;

        return RefundExceptionResponse.builder()
                .requestId(r.getRequestId())
                .bookingId(r.getBooking().getBookingId())
                .stadiumName(stadiumName)
                .reservationDate(reservationDate)
                .customerId(r.getCustomer().getUserId())
                .customerName(r.getCustomer().getFullName())
                .reason(r.getReason())
                .evidenceUrl(r.getEvidenceUrl())
                .status(r.getStatus())
                .ownerNote(r.getOwnerNote())
                .adminNote(r.getAdminNote())
                .refundPercent(r.getRefundPercent())
                .createdAt(r.getCreatedAt())
                .ownerReviewedAt(r.getOwnerReviewedAt())
                .adminReviewedAt(r.getAdminReviewedAt())
                .expiresAt(r.getExpiresAt())
                .canEscalate(canEscalate)
                .isExpired(expired)
                .build();
    }

    private PageResponse<RefundExceptionResponse> toPageResponse(Page<RefundExceptionRequest> page) {
        return PageResponse.of(page, page.getContent().stream().map(this::toResponse).toList());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + email));
    }

    private Booking findBookingById(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân #" + bookingId));
    }

    private RefundExceptionRequest findRequestById(Integer requestId) {
        return refundExceptionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu ngoại lệ #" + requestId));
    }
}
