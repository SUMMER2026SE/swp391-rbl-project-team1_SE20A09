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
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.RefundService;
import com.sportvenue.service.PaymentService;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.util.AfterCommitExecutor;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AfterCommitExecutor afterCommitExecutor;

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
            notificationService.publishNotificationEvent(
                    finalBooking.getUser().getUserId(),
                    "Hoàn tiền thành công",
                    String.format("Đơn đặt sân #%d đã được hoàn tiền %s VNĐ.", 
                            finalBooking.getBookingId(), 
                            java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN")).format(ctx.calculation.getAmount())),
                    com.sportvenue.entity.enums.NotificationType.PAYMENT,
                    String.valueOf(finalBooking.getBookingId())
            );
        } catch (Exception e) {
            log.error("Failed to publish refund notification for booking {}", finalBooking.getBookingId(), e);
        }

        afterCommitExecutor.execute(() -> {
            try {
                emailService.sendRefundEmail(
                        finalBooking.getUser().getEmail(),
                        finalBooking.getUser().getFirstName() + " " + finalBooking.getUser().getLastName(),
                        finalBooking.getStadium().getStadiumName(),
                        finalBooking.getBookingId(),
                        ctx.calculation.getAmount(),
                        ctx.calculation.getPercentage(),
                        finalBooking.getTotalPrice()
                );
            } catch (Exception e) {
                log.error("Failed to send refund email for booking {}", finalBooking.getBookingId(), e);
            }
        });

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
            paymentRepository.findRefundPaymentByBookingId(bookingId).ifPresent(p -> {
                if (p.getPaymentStatus() == TransactionStatus.PENDING || p.getPaymentStatus() == TransactionStatus.SUCCESS) {
                    throw new BadRequestException("Yêu cầu hoàn tiền đang được xử lý hoặc đã thành công.");
                }
            });

            Payment originalPayment = paymentRepository.findSuccessPaymentsByBookingId(bookingId)
                    .stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu"));

            RefundCalculation calculation = calculateRefund(booking, originalPayment, request.getReasonType(), request.getProofUrl(), false);

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

    private void processGatewayRefundTx(RefundProcessContext ctx, Integer bookingId, String reason) {
        boolean gatewaySuccess = false;
        try {
            paymentService.processRefund(ctx.originalPayment, ctx.calculation.getAmount(), reason);
            gatewaySuccess = true;
        } catch (Exception e) {
            log.error("Refund gateway failed for booking {}", bookingId, e);
        }

        final boolean finalSuccess = gatewaySuccess;
        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(ctx.refundPayment.getPaymentId()).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(finalSuccess ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
                paymentRepository.save(payment);
            }
            if (finalSuccess) {
                Booking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking != null) {
                    updateBookingAndReleaseSlot(booking, reason);
                }
            }
            return null;
        });

        if (!gatewaySuccess) {
            throw new BadRequestException(
                    "Lỗi hoàn tiền qua cổng thanh toán. Giao dịch hủy đơn thất bại. Vui lòng thử lại sau.");
        }
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

    private RefundCalculation calculateRefund(Booking booking, Payment originalPayment,
            RefundReasonType reasonType, String proofUrl, boolean isPreview) {
        BigDecimal baseAmount = originalPayment != null ? originalPayment.getAmount() : booking.getTotalPrice();

        if (reasonType == RefundReasonType.OWNER_FAULT) {
            if (!isPreview && (proofUrl == null || proofUrl.trim().isEmpty())) {
                throw new BadRequestException("Bắt buộc phải cung cấp bằng chứng (ảnh/mô tả) khi lỗi do chủ sân");
            }
            return new RefundCalculation(100, baseAmount);
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
            refundAmount = baseAmount.multiply(new BigDecimal("0.5"));
        } else {
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
        }

        return new RefundCalculation(refundPercentage, refundAmount);
    }

    private void updateBookingAndReleaseSlot(Booking booking, String reason) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        if (reason != null && !reason.isBlank()) {
            booking.setNote("Lý do hủy hoàn tiền: " + reason.trim());
        }
        bookingRepository.save(booking);

        TimeSlot slot = booking.getSlot();
        slot.setSlotStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);
    }

    private RefundResponse buildRefundResponse(Booking booking, RefundCalculation calc, String reason) {
        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(booking.getStadium().getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime(booking))
                .originalPrice(booking.getTotalPrice())
                .refundAmount(calc.getAmount())
                .refundPercentage(calc.getPercentage())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(LocalDateTime.now())
                .reason(reason != null ? reason.trim() : "")
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

        // 4. Tìm giao dịch thanh toán gốc (SUCCESS, amount > 0, mới nhất)
        Payment originalPayment = paymentRepository.findSuccessPaymentsByBookingId(bookingId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu"));

        // 5. Áp dụng chính sách hoàn tiền
        RefundCalculation calculation = calculateRefund(booking, originalPayment, reasonType, null, true);

        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(booking.getStadium().getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime(booking))
                .originalPrice(booking.getTotalPrice())
                .refundAmount(calculation.getAmount())
                .refundPercentage(calculation.getPercentage())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(LocalDateTime.now())
                .reason("Xem trước hoàn tiền")
                .build();
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

        // 4. Tìm giao dịch thanh toán gốc
        Payment originalPayment = paymentRepository.findSuccessPaymentsByBookingId(bookingId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu"));

        // 5. Áp dụng chính sách hoàn tiền
        RefundCalculation calculation = calculateRefund(booking, originalPayment, RefundReasonType.CUSTOMER_REQUEST, null, true);

        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(booking.getStadium().getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime(booking))
                .originalPrice(booking.getTotalPrice())
                .refundAmount(calculation.getAmount())
                .refundPercentage(calculation.getPercentage())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(LocalDateTime.now())
                .reason("Xem trước hoàn tiền")
                .build();
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

        // Lấy thông tin Refund từ DB để trả về
        Payment refundPayment = paymentRepository.findSuccessPaymentsByBookingId(bookingId)
                .stream().filter(p -> p.getAmount().compareTo(BigDecimal.ZERO) < 0).findFirst().orElse(null);

        BigDecimal refundAmount = refundPayment != null ? refundPayment.getAmount().negate() : BigDecimal.ZERO;
        BigDecimal originalPrice = booking.getTotalPrice();
        int refundPercentage = 0;
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            refundPercentage = refundAmount.multiply(new BigDecimal("100")).divide(originalPrice, 0, java.math.RoundingMode.HALF_UP).intValue();
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
                .processedAt(refundPayment != null ? refundPayment.getPaidAt() : booking.getBookingDate())
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
        java.util.Map<Integer, BigDecimal> refundMap = new java.util.HashMap<>();
        if (!bookingIds.isEmpty()) {
            List<Payment> refundPayments = paymentRepository.findRefundPaymentsByBookingIds(bookingIds);
            for (Payment p : refundPayments) {
                if (p.getBooking() != null && p.getAmount() != null) {
                    Integer bid = p.getBooking().getBookingId();
                    BigDecimal amt = p.getAmount().abs();
                    refundMap.put(bid, refundMap.getOrDefault(bid, BigDecimal.ZERO).add(amt));
                }
            }
        }

        return bookings.map(b -> {
            String customerName = b.getUser().getFirstName() + " " + b.getUser().getLastName();
            OwnerBookingResponse.CustomerInfo customerInfo = OwnerBookingResponse.CustomerInfo.builder()
                    .name(customerName)
                    .phone(b.getUser().getPhoneNumber())
                    .email(b.getUser().getEmail())
                    .build();

            String startTimeStr = b.getSlot().getStartTime().toString();
            String endTimeStr = b.getSlot().getEndTime().toString();

            BigDecimal refundAmt = refundMap.getOrDefault(b.getBookingId(), BigDecimal.ZERO);

            return OwnerBookingResponse.builder()
                    .id(b.getBookingId())
                    .displayId("BK" + String.format("%06d", b.getBookingId()))
                    .customer(customerInfo)
                    .venue(b.getStadium().getStadiumName())
                    .date(b.getReservationDate().toString())
                    .time(startTimeStr + " - " + endTimeStr)
                    .amount(b.getTotalPrice())
                    .refundAmount(refundAmt)
                    .paymentStatus(b.getPaymentStatus().name().toLowerCase())
                    .status(b.getBookingStatus().name().toLowerCase())
                    .notes(b.getNote() != null ? b.getNote() : "")
                    .playTimeRaw(playTime(b).toString())
                    .build();
        });
    }

    @Getter
    @RequiredArgsConstructor
    private static class RefundCalculation {
        private final int percentage;
        private final BigDecimal amount;
    }
}
