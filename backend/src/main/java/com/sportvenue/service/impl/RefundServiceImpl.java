package com.sportvenue.service.impl;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.OwnerBookingResponse;
import com.sportvenue.dto.response.RefundResponse;

import java.util.List;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.entity.enums.RefundReasonType;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.RefundService;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.CustomerNotificationService;
import com.sportvenue.util.AfterCommitExecutor;
import com.sportvenue.util.StadiumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.transaction.support.TransactionTemplate;
import com.sportvenue.service.WalletService;
import com.sportvenue.entity.enums.WalletTransactionType;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final TransactionTemplate transactionTemplate;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final CustomerNotificationService customerNotificationService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final WalletService walletService;
    private final com.sportvenue.repository.WalletTransactionRepository walletTransactionRepository;

    @Override
    public RefundResponse processRefund(Integer bookingId, RefundRequest request, String ownerEmail) {
        log.info("Starting processRefund for bookingId: {} by Owner: {}", bookingId, ownerEmail);

        RefundProcessContext ctx = processLocalRefundTx(bookingId, ownerEmail, request);

        if (ctx.calculation.getAmount().compareTo(BigDecimal.ZERO) > 0 && ctx.refundPayment != null) {
            processGatewayRefundTx(ctx, bookingId, request.getReason());
        }

        log.info("Successfully processed refund for booking ID: {}. Refund Amount: {} ({}%)",
                bookingId, ctx.calculation.getAmount(), ctx.calculation.getPercentage());

        Booking finalBooking = transactionTemplate
                .execute(status -> bookingRepository.findById(bookingId).orElse(ctx.booking));

        try {
            customerNotificationService.notifyRefundProcessed(finalBooking.getUser().getUserId(), ctx.refundPayment, ctx.calculation.getAmount());
        } catch (Exception e) {
            log.error("Failed to publish refund notification for booking {}", finalBooking.getBookingId(), e);
        }

        return buildRefundResponse(finalBooking, ctx.calculation, request.getReason());
    }

    private RefundProcessContext processLocalRefundTx(Integer bookingId, String ownerEmail, RefundRequest request) {
        return transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + ownerEmail));

            Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

            Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân ID: " + bookingId));

            validateOwnershipAndStatus(booking, owner);

            // Kiểm tra xem đã có giao dịch hoàn tiền nào (PENDING hoặc SUCCESS) đang tồn tại chưa để tránh Double Refund
            paymentRepository.findRefundPaymentByBookingId(bookingId).stream().findFirst().ifPresent(p -> {
                if (p.getPaymentStatus() == TransactionStatus.PENDING || p.getPaymentStatus() == TransactionStatus.SUCCESS) {
                    throw new BadRequestException("Yêu cầu hoàn tiền đang được xử lý hoặc đã thành công.");
                }
            });

            List<Payment> successPayments = paymentRepository.findSuccessPaymentsByBookingId(bookingId);
            if (successPayments.isEmpty()) {
                throw new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu");
            }
            Payment originalPayment = pickReferencePayment(successPayments);
            BigDecimal totalPaid = sumPaidAmount(successPayments);

            RefundCalculation calculation = calculateRefund(booking, totalPaid, request.getReasonType(), request.getProofUrl(), false);

            Payment refundPayment = null;
            if (calculation.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                refundPayment = Payment.builder()
                        .booking(booking)
                        .paymentMethod(originalPayment.getPaymentMethod())
                        .amount(calculation.getAmount().negate())
                        .transactionCode("RFND_" + originalPayment.getTransactionCode())
                        .paymentStatus(TransactionStatus.PENDING)
                        .paidAt(LocalDateTime.now())
                        .reasonType(request.getReasonType())
                        .proofUrl(request.getProofUrl())
                        .build();
                refundPayment = paymentRepository.save(refundPayment);
            } else {
                updateBookingAndReleaseSlot(booking, request.getReason());
            }

            return new RefundProcessContext(booking, originalPayment, calculation, refundPayment);
        });
    }

    /**
     * Hoàn tiền cho Customer — credit thẳng vào Ví nội bộ thay vì gọi gateway VNPay (mock, không
     * làm gì thật). Không còn network call nào để chờ, nên toàn bộ cập nhật Payment + Booking +
     * Owner/Platform wallet + Customer wallet chạy chung 1 transaction ACID duy nhất — nếu bất kỳ
     * bước nào lỗi (vd lỗi DB khi credit ví), cả giao dịch hủy đơn rollback theo, tránh trạng thái
     * nửa vời "đã hủy nhưng ví chưa nhận tiền".
     */
    private void processGatewayRefundTx(RefundProcessContext ctx, Integer bookingId, String reason) {
        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(ctx.refundPayment.getPaymentId()).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(TransactionStatus.SUCCESS);
                paymentRepository.save(payment);
            }

            Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
            if (booking != null) {
                updateBookingAndReleaseSlot(booking, reason);

                BigDecimal refundAmt = ctx.calculation.getAmount();
                BigDecimal serviceFee = booking.getServiceFee() != null ? booking.getServiceFee() : BigDecimal.ZERO;

                // Ghi nhận trừ tiền từ ví nội bộ Owner/Platform — giữ nguyên logic, không đổi.
                Owner resolvedOwner = booking.getStadium() != null ? booking.getStadium().resolveOwner() : null;
                if (resolvedOwner != null) {
                    if (ctx.calculation.isIncludesServiceFee()) {
                        // OWNER_FAULT: Owner bị trừ venuePrice, platform trả lại serviceFee cho khách
                        BigDecimal ownerDebit = refundAmt.subtract(serviceFee);
                        walletService.recordOwnerTransaction(
                                resolvedOwner.getOwnerId(),
                                ownerDebit.negate(),
                                booking.getBookingId(),
                                WalletTransactionType.REFUND_DEBIT,
                                "Khách hoàn tiền do lỗi chủ sân (Owner Fault)"
                        );
                        if (serviceFee.compareTo(BigDecimal.ZERO) > 0) {
                            walletService.recordPlatformTransaction(
                                    serviceFee.negate(),
                                    booking.getBookingId(),
                                    WalletTransactionType.REFUND_FEE_DEBIT,
                                    "Platform hoàn lại phí dịch vụ đơn #" + booking.getBookingId()
                            );
                        }
                    } else {
                        // Huỷ bình thường: Chỉ trừ ví Owner số tiền thực hoàn (đã trừ phí dịch vụ)
                        walletService.recordOwnerTransaction(
                                resolvedOwner.getOwnerId(),
                                refundAmt.negate(),
                                booking.getBookingId(),
                                WalletTransactionType.REFUND_DEBIT,
                                "Khách huỷ đặt sân tự động (Tiền hoàn đã khấu trừ phí dịch vụ)"
                        );
                    }
                }

                // Credit ví Customer — thay cho gọi gateway VNPay, bất kể đơn gốc trả bằng
                // CASH/VNPAY/WALLET đều hoàn về đây.
                walletService.recordCustomerTransaction(
                        booking.getUser().getUserId(),
                        refundAmt,
                        booking.getBookingId(),
                        WalletTransactionType.CUSTOMER_REFUND_CREDIT,
                        "Hoàn tiền huỷ đơn đặt sân #" + booking.getBookingId()
                );
            }
            return null;
        });
    }

    @RequiredArgsConstructor
    private static class RefundProcessContext {
        final Booking booking;
        final Payment originalPayment;
        final RefundCalculation calculation;
        final Payment refundPayment;
    }

    private void validateOwnershipAndStatus(Booking booking, Owner owner) {
        Stadium stadium = booking.getStadium();
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            log.warn("Security Alert! Owner ID {} tried to access booking ID {} of Owner ID {}",
                    owner.getOwnerId(), booking.getBookingId(),
                    resolvedOwner != null ? resolvedOwner.getOwnerId() : null);
            throw new ForbiddenException("Bạn không có quyền quản lý đơn đặt sân này!");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đơn đặt sân này đã ở trạng thái Hủy (CANCELLED)");
        }
        if (Boolean.TRUE.equals(booking.getIsWalkIn())) {
            throw new BadRequestException("Không thể hoàn/hủy cho đơn khách vãng lai thanh toán tại sân");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn đặt sân đã hoàn thành (COMPLETED)");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID && booking.getPaymentStatus() != PaymentStatus.DEPOSITED) {
            throw new BadRequestException(
                    "Chỉ có thể hoàn tiền cho những đơn đặt sân đã thanh toán (PAID hoặc DEPOSITED)");
        }
    }

    private static LocalDateTime playTime(Booking booking) {
        return LocalDateTime.of(booking.getReservationDate(), booking.getSlot().getStartTime());
    }

    /**
     * Chọn payment tham chiếu cho gateway/phương thức hoàn tiền — ưu tiên VNPay (đơn cọc có thể có
     * 2 payment SUCCESS: VNPay lúc cọc + CASH lúc thu nốt; chỉ phần VNPay mới cần gọi gateway thật).
     */
    static Payment pickReferencePayment(List<Payment> successPayments) {
        return successPayments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.VNPAY)
                .findFirst()
                .orElse(successPayments.get(0));
    }

    /**
     * Tổng tiền thực tế đã thu qua TẤT CẢ payment SUCCESS của booking — không dùng 1 payment đơn lẻ
     * (.findFirst()) vì đơn cọc đã thu nốt có 2 dòng Payment, lấy thiếu 1 dòng sẽ hoàn thiếu tiền.
     */
    static BigDecimal sumPaidAmount(List<Payment> successPayments) {
        return successPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static RefundCalculation calculateRefund(Booking booking, BigDecimal paidAmount,
            RefundReasonType reasonType, String proofUrl, boolean isPreview) {
        if (reasonType == RefundReasonType.OWNER_FAULT) {
            if (!isPreview && (proofUrl == null || proofUrl.trim().isEmpty())) {
                throw new BadRequestException("Bắt buộc phải cung cấp bằng chứng (ảnh/mô tả) khi lỗi do chủ sân");
            }
            return new RefundCalculation(100, paidAmount, true);
        }

        // Đặt cọc bị khách tự hủy -> giữ chỗ, KHÔNG hoàn lại (đúng bản chất tiền cọc trong thực
        // tế: cam kết giữ chỗ, mất nếu không đến), bất kể còn bao lâu tới giờ chơi — khác hẳn
        // thanh toán đầy đủ vẫn áp tiering 24h/12h bên dưới. Lỗi do sân (nhánh OWNER_FAULT phía
        // trên) vẫn hoàn 100% cọc như bình thường vì không phải lỗi của khách.
        if (booking.getPaymentStatus() == PaymentStatus.DEPOSITED) {
            return new RefundCalculation(0, BigDecimal.ZERO, false);
        }

        // Khách tự hủy -> Phí dịch vụ không hoàn trả (non-refundable)
        BigDecimal serviceFee = booking.getServiceFee() != null ? booking.getServiceFee() : BigDecimal.ZERO;
        BigDecimal baseAmount = paidAmount.subtract(serviceFee);
        if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            baseAmount = BigDecimal.ZERO;
        }

        LocalDateTime playTime = playTime(booking);
        LocalDateTime now = LocalDateTime.now();
        double hoursDiff = (double) java.time.Duration.between(now, playTime).toMinutes() / 60.0;

        BigDecimal refundAmount;
        int refundPercentage;

        if (hoursDiff >= 24.0) {
            refundPercentage = 100;
            refundAmount = baseAmount;
        } else if (hoursDiff >= 12.0) {
            refundPercentage = 50;
            refundAmount = baseAmount.multiply(new BigDecimal("0.5")).setScale(0, java.math.RoundingMode.HALF_UP);
        } else {
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
        }

        return new RefundCalculation(refundPercentage, refundAmount, false);
    }

    private void updateBookingAndReleaseSlot(Booking booking, String reason) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        if (reason != null && !reason.isBlank()) {
            booking.setNote("Lý do hủy hoàn tiền: " + reason.trim());
            booking.setCancelReason(reason.trim());
        }
        bookingRepository.save(booking);

        TimeSlot slot = booking.getSlot();
        slot.setSlotStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);
    }

    private RefundResponse buildRefundResponse(Booking booking, RefundCalculation calc, String reason) {
        String policyDesc = "";
        if (calc.isIncludesServiceFee()) {
            policyDesc = "Hủy do lỗi chủ sân: Hoàn lại toàn bộ 100% số tiền đã cọc/thanh toán (bao gồm cả phí dịch vụ).";
        } else if (booking.getPaymentStatus() == PaymentStatus.DEPOSITED) {
            policyDesc = "Đơn đặt cọc (30%): Khách tự hủy mất 100% tiền đặt cọc (tiền cọc dùng để giữ chỗ và không hoàn lại).";
        } else {
            if (calc.getPercentage() == 100) {
                policyDesc = "Hủy trước 24 giờ: Hoàn lại 100% giá trị sân (phí dịch vụ hệ thống không hoàn lại).";
            } else if (calc.getPercentage() == 50) {
                policyDesc = "Hủy trước từ 12 đến 24 giờ: Hoàn lại 50% giá trị sân (phí dịch vụ hệ thống không hoàn lại).";
            } else {
                policyDesc = "Hủy dưới 12 giờ: Hoàn lại 0% (khách tự hủy sát giờ chơi).";
            }
        }

        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(booking.getStadium().getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime(booking))
                .originalPrice(booking.getTotalPrice())
                .serviceFee(booking.getServiceFee())
                .refundAmount(calc.getAmount())
                .refundPercentage(calc.getPercentage())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(LocalDateTime.now())
                .reason(reason != null ? reason.trim() : "")
                .cancellationPolicyDescription(policyDesc)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public RefundResponse previewRefund(Integer bookingId, RefundReasonType reasonType, String ownerEmail) {
        log.info("Starting previewRefund for bookingId: {} by Owner: {}", bookingId, ownerEmail);

        // 1. Tìm thông tin User và Owner profile
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + ownerEmail));

        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        // 2. Tìm Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân ID: " + bookingId));

        // 3. Kiểm tra tính hợp lệ và quyền sở hữu
        validateOwnershipAndStatus(booking, owner);

        // 4. Tìm giao dịch thanh toán gốc — SUM tất cả payment SUCCESS (đơn cọc đã thu nốt có 2 dòng)
        List<Payment> successPayments = paymentRepository.findSuccessPaymentsByBookingId(bookingId);
        if (successPayments.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu");
        }
        BigDecimal totalPaid = sumPaidAmount(successPayments);

        // 5. Áp dụng chính sách hoàn tiền
        RefundCalculation calculation = calculateRefund(booking, totalPaid, reasonType, null, true);

        return buildRefundResponse(booking, calculation, "Xem trước hoàn tiền");
    }

    @Transactional(readOnly = true)
    @Override
    public RefundResponse previewRefundForCustomer(Integer bookingId, String customerEmail) {
        log.info("Starting previewRefundForCustomer for bookingId: {} by Customer: {}", bookingId, customerEmail);

        // 1. Tìm thông tin User
        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + customerEmail));

        // 2. Tìm Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân ID: " + bookingId));

        // 3. Kiểm tra tính hợp lệ
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền xem trước hoàn tiền đơn đặt sân này");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đơn đặt sân này đã ở trạng thái Hủy (CANCELLED)");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn đặt sân đã hoàn thành (COMPLETED)");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID && booking.getPaymentStatus() != PaymentStatus.DEPOSITED) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho những đơn đặt sân đã thanh toán (PAID hoặc DEPOSITED)");
        }

        // 4. Tìm giao dịch thanh toán gốc — SUM tất cả payment SUCCESS (đơn cọc đã thu nốt có 2 dòng)
        List<Payment> successPaymentsForPreview = paymentRepository.findSuccessPaymentsByBookingId(bookingId);
        if (successPaymentsForPreview.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu");
        }
        BigDecimal totalPaidForPreview = sumPaidAmount(successPaymentsForPreview);

        // 5. Áp dụng chính sách hoàn tiền
        RefundCalculation calculation = calculateRefund(booking, totalPaidForPreview, RefundReasonType.CUSTOMER_REQUEST, null, true);

        return buildRefundResponse(booking, calculation, "Xem trước hoàn tiền");
    }

    @Transactional(readOnly = true)
    @Override
    public RefundResponse getRefundResponse(Integer bookingId, String ownerEmail) {
        log.info("Starting getRefundResponse for bookingId: {} by Owner: {}", bookingId, ownerEmail);

        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + ownerEmail));

        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân ID: " + bookingId));

        Stadium stadium = booking.getStadium();
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("Bạn không có quyền quản lý đơn đặt sân này!");
        }

        // Lấy thông tin Refund từ DB để trả về — findSuccessPaymentsByBookingId chỉ trả amount > 0
        // (xem PaymentRepository.java:29), nên filter "amount < 0" lên kết quả đó LUÔN RỖNG. Payment
        // hoàn tiền (âm) phải tìm qua findRefundPaymentByBookingId (amount <= 0), và SUM tất cả dòng
        // SUCCESS (không chỉ lấy 1 dòng) — vì "Yêu cầu ngoại lệ" có thể tạo thêm payment hoàn bổ sung
        // (top-up) sau lần hoàn gốc.
        List<Payment> refundPayments = paymentRepository.findRefundPaymentByBookingId(bookingId).stream()
                .filter(p -> p.getPaymentStatus() == TransactionStatus.SUCCESS)
                .toList();
        BigDecimal refundAmount = refundPayments.stream()
                .map(p -> p.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime processedAt = refundPayments.stream()
                .map(Payment::getPaidAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(booking.getBookingDate());

        // % LUÔN tính trên tổng tiền THỰC TẾ đã thanh toán (paidAmount), không phải totalPrice —
        // đơn đặt cọc chỉ thu 1 phần totalPrice nên chia cho totalPrice sẽ ra % thấp giả tạo.
        List<Payment> successPayments = paymentRepository.findSuccessPaymentsByBookingId(bookingId);
        BigDecimal paidAmount = sumPaidAmount(successPayments);
        BigDecimal originalPrice = booking.getTotalPrice();
        int refundPercentage = 0;
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundPercentage = refundAmount.multiply(new BigDecimal("100")).divide(paidAmount, 0, java.math.RoundingMode.HALF_UP).intValue();
        }

        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(stadium.getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime(booking))
                .originalPrice(originalPrice)
                .refundAmount(refundAmount)
                .refundPercentage(refundPercentage)
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(processedAt)
                .reason(booking.getNote() != null ? booking.getNote().replace("Lý do hủy hoàn tiền: ", "") : "")
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OwnerBookingResponse> getOwnerBookings(
            String ownerEmail, BookingStatus status, Pageable pageable) {
        log.info("Fetching bookings for owner: {}, status: {}, page: {}",
                ownerEmail, status, pageable.getPageNumber());

        Page<Booking> bookings = bookingRepository.findByOwnerEmailAndStatus(
                ownerEmail, status, pageable);

        List<Integer> bookingIds = bookings.getContent().stream()
                .map(Booking::getBookingId).toList();
        java.util.Map<Integer, BigDecimal> refundMap = buildRefundMap(bookingIds);
        java.util.Map<Integer, BigDecimal> successPaymentMap = buildSuccessPaymentMap(bookingIds);

        return bookings.map(b -> {
            OwnerBookingResponse.CustomerInfo customerInfo = null;
            if (b.getUser() != null) {
                String customerName = b.getUser().getFirstName() + " " + b.getUser().getLastName();
                customerInfo = OwnerBookingResponse.CustomerInfo.builder()
                        .userId(b.getUser().getUserId())
                        .name(customerName)
                        .phone(b.getUser().getPhoneNumber())
                        .email(b.getUser().getEmail())
                        .build();
            } else {
                customerInfo = OwnerBookingResponse.CustomerInfo.builder()
                        .name("Khách vãng lai")
                        .build();
            }

            String startTimeStr = b.getSlot().getStartTime().toString();
            String endTimeStr = b.getSlot().getEndTime().toString();

            BigDecimal refundAmt = refundMap.getOrDefault(b.getBookingId(), BigDecimal.ZERO);
            BigDecimal successPaid = successPaymentMap.getOrDefault(b.getBookingId(), BigDecimal.ZERO);

            // Nếu đơn bị hủy, số tiền thực tế khách đã trả qua cổng thanh toán chỉ là số tiền thanh toán thành công (Paid hoặc Deposited).
            // Nếu không bị hủy (COMPLETED, CONFIRMED...), khách sẽ thanh toán đủ b.getTotalPrice() (online hoặc tiền mặt).
            BigDecimal bookingAmt = b.getTotalPrice();
            if (b.getBookingStatus() == com.sportvenue.entity.enums.BookingStatus.CANCELLED) {
                bookingAmt = successPaid;
            }

            BigDecimal serviceFee = b.getServiceFee() != null ? b.getServiceFee() : BigDecimal.ZERO;

            return OwnerBookingResponse.builder()
                    .id(b.getBookingId())
                    .displayId("BK" + String.format("%06d", b.getBookingId()))
                    .customer(customerInfo)
                    .stadiumId(b.getStadium().getStadiumId())
                    .venue(b.getStadium().getStadiumName())
                    .complexName(StadiumUtils.resolveComplexName(b.getStadium()))
                    .date(b.getReservationDate().toString())
                    .time(startTimeStr + " - " + endTimeStr)
                    .amount(bookingAmt)
                    .refundAmount(refundAmt)
                    .serviceFee(serviceFee)
                    .paidAmount(successPaid)
                    .paymentStatus(b.getPaymentStatus().name().toLowerCase())
                    .status(b.getBookingStatus().name().toLowerCase())
                    .notes(b.getNote() != null ? b.getNote() : "")
                    .playTimeRaw(playTime(b).toString())
                    .isWalkIn(b.getIsWalkIn())
                    .build();
        });
    }

    private java.util.Map<Integer, BigDecimal> buildRefundMap(List<Integer> bookingIds) {
        java.util.Map<Integer, BigDecimal> refundMap = new java.util.HashMap<>();
        if (bookingIds.isEmpty()) {
            return refundMap;
        }
        for (Payment p : paymentRepository.findRefundPaymentsByBookingIds(bookingIds)) {
            if (p.getBooking() != null && p.getAmount() != null) {
                Integer bid = p.getBooking().getBookingId();
                refundMap.put(bid, refundMap.getOrDefault(bid, BigDecimal.ZERO).add(p.getAmount().abs()));
            }
        }
        return refundMap;
    }

    private java.util.Map<Integer, BigDecimal> buildSuccessPaymentMap(List<Integer> bookingIds) {
        java.util.Map<Integer, BigDecimal> map = new java.util.HashMap<>();
        if (bookingIds.isEmpty()) {
            return map;
        }
        for (Payment p : paymentRepository.findSuccessPaymentsByBookingIds(bookingIds)) {
            if (p.getBooking() != null && p.getAmount() != null) {
                // SUM (không overwrite) — đơn cọc đã thu nốt có 2 dòng Payment SUCCESS (VNPay cọc +
                // CASH còn lại); .put() ghi đè sẽ làm mất 1 phần tiền đã thu.
                Integer bid = p.getBooking().getBookingId();
                map.put(bid, map.getOrDefault(bid, BigDecimal.ZERO).add(p.getAmount()));
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    @Override
    public com.sportvenue.dto.response.OwnerBookingsSummaryResponse getOwnerBookingsSummary(
            String ownerEmail, BookingStatus status) {
        // ── Nguồn sự thật đã chốt ──────────────────────────────────────────────────────
        // Gross & Refund  → Payment table (tiền thực tế đã thu/đã hoàn qua cổng/cash)
        // Fee             → Owner Wallet ledger (SERVICE_FEE_DEBIT) — không tự tính
        //                   từ Booking.serviceFee vì đơn DEPOSITED chưa thu đủ tiền
        //                   nhưng phí đã bị ghi vào Booking, dẫn đến Fee bị thổi phồng.
        // Net             → Gross - Refund - Fee (tính sau)
        // Không còn vòng lặp Booking — mọi thứ lấy từ aggregate query trực tiếp.
        // ───────────────────────────────────────────────────────────────────────────────

        Owner owner = ownerRepository.findByUserEmail(ownerEmail)
                .orElseThrow(() -> new com.sportvenue.exception.ResourceNotFoundException(
                        "Không tìm thấy owner profile: " + ownerEmail));
        Integer ownerId = owner.getOwnerId();

        // Avoid passing null to JPQL queries to prevent PostgreSQL parameter type inference errors
        LocalDateTime minDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime maxDate = LocalDateTime.of(2100, 1, 1, 0, 0);
        
        // Use a list of statuses to avoid (:status IS NULL OR ...) which crashes PostgreSQL
        java.util.List<BookingStatus> statuses = (status != null) 
                ? java.util.List.of(status) 
                : java.util.Arrays.asList(BookingStatus.values());

        // Gross = tổng payment SUCCESS, amount > 0, thuộc booking của owner
        BigDecimal grossAmount = paymentRepository.sumOwnerGrossByDateRangeAndStatuses(ownerId, minDate, maxDate, statuses);
        if (grossAmount == null) grossAmount = BigDecimal.ZERO;

        // Refund = tổng payment SUCCESS, amount < 0, thuộc booking của owner (giá trị dương)
        BigDecimal refundedAmount = paymentRepository.sumOwnerRefundByDateRangeAndStatuses(ownerId, minDate, maxDate, statuses);
        if (refundedAmount == null) refundedAmount = BigDecimal.ZERO;

        // Fee = tổng SERVICE_FEE_DEBIT trong Owner Wallet ledger (đã xảy ra thực tế)
        BigDecimal serviceFeeTotal = walletTransactionRepository.sumOwnerFeeByTypeDateRangeAndStatuses(
                ownerId,
                com.sportvenue.entity.enums.WalletTransactionType.SERVICE_FEE_DEBIT,
                minDate, maxDate, statuses);
        if (serviceFeeTotal == null) serviceFeeTotal = BigDecimal.ZERO;

        BigDecimal netAmount = grossAmount.subtract(refundedAmount).subtract(serviceFeeTotal);

        log.info("Owner {} summary — Gross={}, Refund={}, Fee={}, Net={}",
                ownerEmail, grossAmount, refundedAmount, serviceFeeTotal, netAmount);

        return com.sportvenue.dto.response.OwnerBookingsSummaryResponse.builder()
                .grossAmount(grossAmount)
                .refundedAmount(refundedAmount)
                .serviceFee(serviceFeeTotal)
                .netAmount(netAmount)
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    static class RefundCalculation {
        private final int percentage;
        private final BigDecimal amount;
        private final boolean includesServiceFee;
    }
}
