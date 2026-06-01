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
        log.info("Starting processRefund for bookingId: {} by Owner user: {}", bookingId, ownerEmail);

        // 1. Tìm thông tin User từ email đăng nhập
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + ownerEmail));

        // 2. Tìm Owner profile từ userId
        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        // 3. Tìm và kiểm tra Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân với ID: " + bookingId));

        // 4. Kiểm tra xem sân của booking này có thuộc sở hữu của Owner hiện tại không
        Stadium stadium = booking.getStadium();
        if (!stadium.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            log.warn("Security Alert! Owner ID {} tried to process refund for booking ID {} belonging to stadium ID {} of Owner ID {}",
                    owner.getOwnerId(), bookingId, stadium.getStadiumId(), stadium.getOwner().getOwnerId());
            throw new BadRequestException("Bạn không có quyền quản lý đơn đặt sân này!");
        }

        // 5. Kiểm tra tính hợp lệ về trạng thái đơn đặt sân
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đơn đặt sân này đã ở trạng thái Hủy (CANCELLED)");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy và hoàn tiền cho đơn đặt sân đã hoàn thành (COMPLETED)");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Chỉ có thể xử lý hoàn tiền cho những đơn đặt sân đã thanh toán thành công (PAID)");
        }

        // 6. Tìm giao dịch thanh toán gốc
        Payment originalPayment = paymentRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán ban đầu cho đơn đặt sân này"));

        // 7. Áp dụng chính sách hoàn tiền dựa trên thời gian hủy
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

        // 8. Cập nhật dữ liệu (Chạy trong Transaction)
        
        // 8.1. Cập nhật trạng thái của Booking sang CANCELLED và REFUNDED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        // 8.2. Giải phóng khung giờ (TimeSlot) sang AVAILABLE để khách khác có thể đặt
        TimeSlot slot = booking.getSlot();
        slot.setSlotStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);

        // 8.3. Nếu số tiền hoàn lại lớn hơn 0, tạo một bản ghi giao dịch thanh toán âm (Refund)
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment refundPayment = Payment.builder()
                    .booking(booking)
                    .paymentMethod(originalPayment.getPaymentMethod())
                    .amount(refundAmount.negate()) // Số tiền âm đại diện cho dòng tiền đi ra khỏi hệ thống
                    .transactionCode("RFND_" + originalPayment.getTransactionCode())
                    .paymentStatus(TransactionStatus.SUCCESS)
                    .paidAt(now)
                    .build();
            paymentRepository.save(refundPayment);
            log.info("Recorded negative refund transaction of amount {} for booking ID {}", refundAmount.negate(), bookingId);
        }

        log.info("Successfully processed refund for booking ID: {}. Refund Amount: {} ({}%)",
                bookingId, refundAmount, refundPercentage);

        // 9. Trả về kết quả
        return RefundResponse.builder()
                .bookingId(booking.getBookingId())
                .stadiumName(stadium.getStadiumName())
                .customerName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .playTime(playTime)
                .originalPrice(booking.getTotalPrice())
                .refundAmount(refundAmount)
                .refundPercentage(refundPercentage)
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .processedAt(now)
                .reason(request.getReason() != null ? request.getReason().trim() : "")
                .build();
    }
}
