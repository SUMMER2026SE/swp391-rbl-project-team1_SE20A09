package com.sportvenue.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler gửi thông báo nhắc lịch chơi cho booking CONFIRMED trong vòng 24h tới.
 *
 * <p>Chạy mỗi 10 phút (fixedDelay = 600_000ms), delay khởi động 90 giây.
 *
 * <p><b>Cơ chế chống duplicate:</b> Sau khi nhắc thành công, set reminderSentAt = now.
 * Query chỉ lấy booking có reminderSentAt IS NULL nên không nhắc trùng.
 *
 * <p><b>Giới hạn kiến trúc:</b> Scheduler chạy in-process, không có distributed lock.
 * Khi deploy nhiều instance, cần tích hợp ShedLock + Redis để tránh nhắc trùng.
 *
 * <p><b>Xử lý lỗi email:</b> MailException được bắt trong EmailService (@Async).
 * Nếu email lỗi, reminderSentAt vẫn được set để tránh retry vô hạn.
 * Thay đổi thiết kế này nếu yêu cầu retry email là bắt buộc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Quét booking CONFIRMED chưa nhắc trong phạm vi ~24h tới,
     * gửi thông báo in-app + email, đánh dấu reminderSentAt.
     */
    @Transactional
    @Scheduled(fixedDelay = 600_000, initialDelay = 90_000)
    public void sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();

        // endDate dùng plusHours(24).toLocalDate() thay vì plusDays(1)
        // để đảm bảo cover đúng 24h tới, tránh bỏ sót booking ở biên ngày
        // (VD: now=23:00 → plusDays(1)=00:00 ngày kia, nhưng plusHours(24)=23:00 ngày mai)
        LocalDate startDate = now.toLocalDate();
        LocalDate endDate = now.plusHours(24).toLocalDate();

        List<Booking> candidates = bookingRepository.findUpcomingUnremindedBookings(startDate, endDate);
        List<Booking> reminded = new ArrayList<>();

        for (Booking b : candidates) {
            if (b.getSlot() == null || b.getUser() == null || b.getStadium() == null) {
                log.warn("BookingReminderScheduler: booking {} missing required associations, skipping",
                        b.getBookingId());
                continue;
            }

            // Lọc chính xác: chỉ nhắc khi giờ bắt đầu nằm trong (now, now+24h]
            LocalDateTime playStart = LocalDateTime.of(b.getReservationDate(), b.getSlot().getStartTime());
            if (!playStart.isAfter(now) || !playStart.isBefore(now.plusHours(24))) {
                continue;
            }

            String stadiumName = b.getStadium().getStadiumName();
            String reservationDateStr = b.getReservationDate().format(DATE_FMT);
            String timeRange = b.getSlot().getStartTime().format(TIME_FMT)
                    + " – " + b.getSlot().getEndTime().format(TIME_FMT);

            // 1. Gửi thông báo in-app
            notificationService.createNotification(
                    b.getUser().getUserId(),
                    "Nhắc lịch chơi",
                    "Bạn có lịch đặt sân " + stadiumName
                            + " vào ngày " + reservationDateStr
                            + " lúc " + timeRange,
                    NotificationType.BOOKING,
                    String.valueOf(b.getBookingId())
            );

            // 2. Gửi email (lỗi email không làm hỏng cả batch — được catch trong @Async method)
            try {
                emailService.sendBookingReminderEmail(
                        b.getUser().getEmail(),
                        stadiumName,
                        reservationDateStr,
                        timeRange
                );
            } catch (Exception e) {
                // Dự phòng nếu EmailService không catch nội bộ
                log.error("BookingReminderScheduler: unexpected error sending email for booking {}: {}",
                        b.getBookingId(), e.getMessage());
            }

            // 3. Đánh dấu đã nhắc (dù email lỗi vẫn set để tránh retry vô hạn)
            b.setReminderSentAt(now);
            reminded.add(b);
        }

        if (!reminded.isEmpty()) {
            bookingRepository.saveAll(reminded);
            log.info("BookingReminderScheduler: sent reminders for {} booking(s)", reminded.size());
        } else {
            log.debug("BookingReminderScheduler: no upcoming bookings to remind at {}", now);
        }
    }
}
