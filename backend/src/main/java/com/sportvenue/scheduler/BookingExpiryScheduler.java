package com.sportvenue.scheduler;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-CUS-01: Scheduler tự huỷ các booking {@code PENDING_PAYMENT} đã quá hạn thanh toán.
 *
 * <p>Chạy mỗi 60 giây — tìm booking có {@code expired_at < now()} và status = PENDING_PAYMENT,
 * chuyển sang CANCELLED, giải phóng slot. Index {@code idx_bookings_pending_payment_expiry}
 * đảm bảo query nhanh ngay cả khi bảng lớn.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Quét booking quá hạn mỗi 60s. Dùng {@code @Scheduled(fixedDelay)} thay vì
     * {@code @Scheduled(fixedRate)} để tránh overlap nếu job trước chưa xong.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void cancelExpiredPendingPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingRepository.findExpiredPendingPayments(now);

        if (expired.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (Booking booking : expired) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setExpiredAt(null);
            // Slot sẽ tự rảnh vì ACTIVE_STATUSES không còn chứa CANCELLED.
            cancelled++;
        }
        bookingRepository.saveAll(expired);

        log.info("⏰ BookingExpiryScheduler: huỷ {} booking PENDING_PAYMENT quá hạn (now={})",
                cancelled, now);
    }
}
