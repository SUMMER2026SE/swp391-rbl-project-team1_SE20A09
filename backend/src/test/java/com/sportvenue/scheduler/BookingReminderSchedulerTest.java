package com.sportvenue.scheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class BookingReminderSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingReminderScheduler scheduler;

    private User user;
    private Stadium stadium;
    private TimeSlot upcomingSlot;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(10)
                .email("player@example.com")
                .firstName("Nguyen")
                .lastName("Van A")
                .phoneNumber("0900000000")
                .passwordHash("hashed")
                .build();

        stadium = Stadium.builder()
                .stadiumId(20)
                .stadiumName("Sân Cầu Lông ABC")
                .build();

        // Slot bắt đầu 2 giờ nữa (trong 24h tới).
        // Nếu LocalTime.now().plusHours(2) vượt qua 00:00, reservationDate phải là ngày mai.
        // Dùng LocalDateTime để tính chính xác rồi extract ra LocalDate + LocalTime.
        java.time.LocalDateTime playStartDT = java.time.LocalDateTime.now().plusHours(2);
        upcomingSlot = TimeSlot.builder()
                .startTime(playStartDT.toLocalTime())
                .endTime(playStartDT.plusHours(1).toLocalTime())
                .build();
        upcomingReservationDate = playStartDT.toLocalDate();
    }

    // reservationDate tương ứng với upcomingSlot (có thể là hôm nay hoặc ngày mai)
    private java.time.LocalDate upcomingReservationDate;

    @Test
    @DisplayName("Booking đủ điều kiện → gửi notification + email + set reminderSentAt + saveAll")
    void sendUpcomingReminders_shouldSendReminderForEligibleBooking() {
        Booking booking = Booking.builder()
                .bookingId(100)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(upcomingReservationDate)
                .slot(upcomingSlot)
                .user(user)
                .stadium(stadium)
                .build();

        when(bookingRepository.findUpcomingUnremindedBookings(any(), any()))
                .thenReturn(List.of(booking));

        scheduler.sendUpcomingReminders();

        // Verify notification gửi với đúng type
        verify(notificationService).createNotification(
                eq(10),
                eq("Nhắc lịch chơi"),
                contains("Sân Cầu Lông ABC"),
                eq(NotificationType.BOOKING),
                eq("100")
        );

        // Verify email được gửi
        verify(emailService).sendBookingReminderEmail(
                eq("player@example.com"),
                eq("Sân Cầu Lông ABC"),
                anyString(),
                anyString()
        );

        // Verify reminderSentAt được set (chống duplicate)
        assertThat(booking.getReminderSentAt()).isNotNull();

        // Verify saveAll được gọi với booking đã nhắc
        verify(bookingRepository).saveAll(argThat(list ->
                ((List<?>) list).contains(booking)
        ));
    }

    @Test
    @DisplayName("Email lỗi → notification vẫn gửi, reminderSentAt vẫn set, saveAll vẫn chạy")
    void sendUpcomingReminders_emailFailure_shouldNotAbortBatch() {
        Booking booking = Booking.builder()
                .bookingId(101)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(upcomingReservationDate)
                .slot(upcomingSlot)
                .user(user)
                .stadium(stadium)
                .build();

        when(bookingRepository.findUpcomingUnremindedBookings(any(), any()))
                .thenReturn(List.of(booking));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendBookingReminderEmail(any(), any(), any(), any());

        scheduler.sendUpcomingReminders();

        // Notification vẫn gửi thành công
        verify(notificationService).createNotification(any(), any(), any(), any(), any());

        // reminderSentAt vẫn được set dù email lỗi
        assertThat(booking.getReminderSentAt()).isNotNull();

        // saveAll vẫn chạy
        verify(bookingRepository).saveAll(any());
    }

    @Test
    @DisplayName("Booking có playStart đã qua → không nhắc (lọc Java, không phải DB)")
    void sendUpcomingReminders_shouldSkipPastPlayStart() {
        java.time.LocalDateTime pastStartDT = java.time.LocalDateTime.now().minusHours(1);
        TimeSlot pastSlot = TimeSlot.builder()
                .startTime(pastStartDT.toLocalTime()) // 1 giờ trước
                .endTime(pastStartDT.plusHours(1).toLocalTime())
                .build();

        Booking booking = Booking.builder()
                .bookingId(102)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(pastStartDT.toLocalDate())
                .slot(pastSlot)
                .user(user)
                .stadium(stadium)
                .build();

        when(bookingRepository.findUpcomingUnremindedBookings(any(), any()))
                .thenReturn(List.of(booking));

        scheduler.sendUpcomingReminders();

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(emailService, never()).sendBookingReminderEmail(any(), any(), any(), any());
        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Không có booking nào → không gửi gì, saveAll không được gọi")
    void sendUpcomingReminders_shouldDoNothingWhenEmpty() {
        when(bookingRepository.findUpcomingUnremindedBookings(any(), any()))
                .thenReturn(List.of());

        scheduler.sendUpcomingReminders();

        verifyNoInteractions(notificationService, emailService);
        verify(bookingRepository, never()).saveAll(any());
    }
}
