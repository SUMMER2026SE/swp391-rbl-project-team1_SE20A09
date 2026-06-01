package com.sportvenue.service.impl;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.RefundResponse;
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
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.RefundService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;

    @Transactional
    @Override
    public RefundResponse processRefund(Integer bookingId, RefundRequest request, String ownerEmail) {
        log.info("Starting processRefund for bookingId: {} by Owner: {}", bookingId, ownerEmail);

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

        // 4. Tìm giao dịch thanh toán gốc
        Payment originalPayment = paymentRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu"));

        // 5. Áp dụng chính sách hoàn tiền
        RefundCalculation calculation = calculateRefund(booking);

        // 6. Cập nhật dữ liệu
        updateBookingAndReleaseSlot(booking);

        // 7. Tạo bản ghi giao dịch âm nếu có số tiền hoàn lại lớn hơn 0
        if (calculation.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            recordRefundTransaction(booking, originalPayment, calculation.getAmount());
        }

        log.info("Successfully processed refund for booking ID: {}. Refund Amount: {} ({}%)",
                bookingId, calculation.getAmount(), calculation.getPercentage());

        return buildRefundResponse(booking, calculation, request.getReason());
    }

    private void validateOwnershipAndStatus(Booking booking, Owner owner) {
        Stadium stadium = booking.getStadium();
        if (!stadium.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            log.warn("Security Alert! Owner ID {} tried to access booking ID {} of Owner ID {}",
                    owner.getOwnerId(), booking.getBookingId(), stadium.getOwner().getOwnerId());
            throw new BadRequestException("Bạn không có quyền quản lý đơn đặt sân này!");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đơn đặt sân này đã ở trạng thái Hủy (CANCELLED)");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn đặt sân đã hoàn thành (COMPLETED)");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho những đơn đặt sân đã thanh toán (PAID)");
        }
    }

    private RefundCalculation calculateRefund(Booking booking) {
        LocalDateTime playTime = booking.getSlot().getStartTime();
        LocalDateTime now = LocalDateTime.now();
        long hoursDiff = java.time.Duration.between(now, playTime).toHours();

        BigDecimal refundAmount;
        int refundPercentage;

        if (hoursDiff >= 24) {
            refundPercentage = 100;
            refundAmount = booking.getTotalPrice();
        } else if (hoursDiff >= 12) {
            refundPercentage = 50;
            refundAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.5));
        } else {
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
        }

        return new RefundCalculation(refundPercentage, refundAmount);
    }

    private void updateBookingAndReleaseSlot(Booking booking) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        TimeSlot slot = booking.getSlot();
        slot.setSlotStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);
    }

    private void recordRefundTransaction(Booking booking, Payment originalPayment, BigDecimal refundAmount) {
        Payment refundPayment = Payment.builder()
                .booking(booking)
                .paymentMethod(originalPayment.getPaymentMethod())
                .amount(refundAmount.negate())
                .transactionCode("RFND_" + originalPayment.getTransactionCode())
                .paymentStatus(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(refundPayment);
        log.info("Recorded negative refund transaction of amount {} for booking ID {}", 
                refundAmount.negate(), booking.getBookingId());
    }

    private RefundResponse buildRefundResponse(Booking booking, RefundCalculation calc, String reason) {
        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(booking.getStadium().getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(booking.getSlot().getStartTime())
                .originalPrice(booking.getTotalPrice())
                .refundAmount(calc.getAmount())
                .refundPercentage(calc.getPercentage())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(LocalDateTime.now())
                .reason(reason != null ? reason.trim() : "")
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    private static class RefundCalculation {
        private final int percentage;
        private final BigDecimal amount;
    }
}
