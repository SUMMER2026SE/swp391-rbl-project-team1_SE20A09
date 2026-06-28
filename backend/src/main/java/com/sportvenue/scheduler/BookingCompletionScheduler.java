package com.sportvenue.scheduler;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler tự động chuyển trạng thái booking CONFIRMED → COMPLETED
 * sau khi thời gian chơi (slot.endTime) đã qua.
 *
 * <p>Chạy mỗi 5 phút (fixedDelay = 300_000ms), delay khởi động 60 giây.
 *
 * <p><b>Giới hạn kiến trúc:</b> Scheduler chạy in-process, không có distributed lock.
 * Khi deploy nhiều instance, cần tích hợp ShedLock + Redis để tránh job chạy trùng.
 * Với single-instance deployment, @Scheduled là đủ.
 *
 * <p><b>Lưu ý scale:</b> Hiện tại query lấy tất cả booking CONFIRMED có date <= hôm nay.
 * Nếu dữ liệu lớn, cân nhắc thêm giới hạn dưới (VD: reservationDate >= minusDays(30))
 * hoặc xử lý saveAll theo chunks để tránh lock DB lâu.
 *
 * <p><b>Tiên quyết cho:</b> Review submission, Complaint creation, Owner revenue reports.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Quét các booking CONFIRMED có ngày đặt sân <= hôm nay,
     * kiểm tra chính xác endTime ở Java, và chuyển sang COMPLETED.
     */
    @Transactional
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void completePastBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<Booking> candidates = bookingRepository.findConfirmedPastPlayTime(today);
        List<Booking> toComplete = new ArrayList<>();

        for (Booking b : candidates) {
            // Null-check slot phòng trường hợp dữ liệu bất nhất (FK constraint nên đảm bảo)
            if (b.getSlot() == null) {
                log.warn("BookingCompletionScheduler: booking {} has null slot, skipping", b.getBookingId());
                continue;
            }

            // Lọc chính xác: chỉ complete khi giờ kết thúc slot đã qua
            LocalDateTime endDateTime = LocalDateTime.of(b.getReservationDate(), b.getSlot().getEndTime());
            if (endDateTime.isBefore(now)) {
                b.setBookingStatus(BookingStatus.COMPLETED);
                toComplete.add(b);
            }
        }

        if (!toComplete.isEmpty()) {
            bookingRepository.saveAll(toComplete);
            log.info("BookingCompletionScheduler: marked {} booking(s) as COMPLETED", toComplete.size());
        } else {
            log.debug("BookingCompletionScheduler: no bookings to complete at {}", now);
        }
    }
}
