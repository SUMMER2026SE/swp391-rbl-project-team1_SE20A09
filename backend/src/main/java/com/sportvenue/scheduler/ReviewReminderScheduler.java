package com.sportvenue.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sportvenue.entity.Booking;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.service.CustomerNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler gửi nhắc đánh giá sân cho khách hàng 1-2 ngày sau khi chơi.
 *
 * <p>Chạy mỗi 6 giờ (fixedDelay = 21_600_000ms), delay khởi động 120 giây.
 *
 * <p><b>Cơ chế chống duplicate:</b> Sau khi gửi thành công, set reviewReminderSentAt = now.
 * Query chỉ lấy booking có reviewReminderSentAt IS NULL nên không nhắc trùng.
 *
 * <p><b>Điều kiện nhắc:</b>
 * <ul>
 *   <li>Booking status = COMPLETED</li>
 *   <li>reservationDate nằm trong khoảng [hôm nay - 2 ngày, hôm nay - 1 ngày]</li>
 *   <li>Chưa có Review cho booking này</li>
 *   <li>reviewReminderSentAt IS NULL</li>
 * </ul>
 *
 * <p><b>Giới hạn kiến trúc:</b> Scheduler chạy in-process, không có distributed lock.
 * Khi deploy nhiều instance, cần tích hợp ShedLock để tránh gửi trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReminderScheduler {

    private final BookingRepository bookingRepository;
    private final CustomerNotificationService customerNotificationService;

    /**
     * Quét booking COMPLETED trong 1-2 ngày trước chưa được review và chưa nhắc,
     * gửi thông báo REVIEW_REMINDER (in-app + email), đánh dấu reviewReminderSentAt.
     */
    @Transactional
    @Scheduled(fixedDelay = 21_600_000, initialDelay = 120_000)
    public void sendReviewReminders() {
        LocalDate today = LocalDate.now();
        // Nhắc các booking có reservationDate từ 1-2 ngày trước
        LocalDate startDate = today.minusDays(2);
        LocalDate endDate = today.minusDays(1);

        List<Booking> candidates = bookingRepository.findCompletedUnreviewedUnremindedBookings(startDate, endDate);
        List<Booking> reminded = new ArrayList<>();

        for (Booking booking : candidates) {
            if (booking.getUser() == null || booking.getStadium() == null) {
                log.warn("ReviewReminderScheduler: booking {} missing user or stadium, skipping",
                        booking.getBookingId());
                continue;
            }

            try {
                customerNotificationService.notifyReviewReminder(
                        booking.getUser().getUserId(), booking);
                booking.setReviewReminderSentAt(LocalDateTime.now());
                reminded.add(booking);
            } catch (Exception e) {
                log.error("ReviewReminderScheduler: failed to send review reminder for booking {}: {}",
                        booking.getBookingId(), e.getMessage());
                // Không set reviewReminderSentAt khi lỗi → sẽ retry lần chạy sau
            }
        }

        if (!reminded.isEmpty()) {
            bookingRepository.saveAll(reminded);
            log.info("ReviewReminderScheduler: sent review reminders for {} booking(s)", reminded.size());
        } else {
            log.debug("ReviewReminderScheduler: no bookings to remind at {}", today);
        }
    }
}
