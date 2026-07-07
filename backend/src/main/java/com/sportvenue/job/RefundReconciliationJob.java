package com.sportvenue.job;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job đối soát (Reconciliation Job) kiểm tra trạng thái các giao dịch hoàn tiền bị kẹt (PENDING)
 * do lỗi server crash hoặc timeout sau khi thực hiện Phase 1 (Local Tx).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TransactionTemplate transactionTemplate;

    // Chạy mỗi 5 phút
    @Scheduled(fixedRate = 300000)
    public void reconcilePendingRefunds() {
        // Quét các giao dịch PENDING bị kẹt quá 5 phút
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Payment> pendingRefunds = paymentRepository.findPendingRefundsOlderThan(threshold);

        if (!pendingRefunds.isEmpty()) {
            log.info("[Reconciliation] Tìm thấy {} giao dịch hoàn tiền bị kẹt (PENDING) trước {}", 
                     pendingRefunds.size(), threshold);
        }

        for (Payment refundPayment : pendingRefunds) {
            try {
                // Gọi API QueryDR của Cổng thanh toán để lấy trạng thái thực tế
                boolean isSuccess = paymentService.checkRefundStatus(refundPayment);
                
                transactionTemplate.execute(status -> {
                    // Update Payment Status
                    refundPayment.setPaymentStatus(isSuccess ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
                    paymentRepository.save(refundPayment);
                    
                    if (isSuccess) {
                        // Update Booking & Slot Status only if Refund was actually successful
                        Booking booking = bookingRepository.findById(refundPayment.getBooking().getBookingId()).orElse(null);
                        if (booking != null) {
                            booking.setBookingStatus(BookingStatus.CANCELLED);
                            booking.setPaymentStatus(PaymentStatus.REFUNDED);
                            booking.setExpiredAt(null);
                            
                            TimeSlot slot = booking.getSlot();
                            if (slot != null && slot.getSlotStatus() == SlotStatus.BOOKED) {
                                slot.setSlotStatus(SlotStatus.AVAILABLE);
                                timeSlotRepository.save(slot);
                            }
                            bookingRepository.save(booking);
                        }
                    }
                    return null;
                });
                log.info("[Reconciliation] Đã đối soát thành công giao dịch {}, trạng thái chốt: {}", 
                         refundPayment.getTransactionCode(), isSuccess ? "SUCCESS" : "FAILED");
            } catch (Exception e) {
                log.error("[Reconciliation] Lỗi khi đối soát giao dịch hoàn tiền {}", refundPayment.getTransactionCode(), e);
            }
        }
    }
}
