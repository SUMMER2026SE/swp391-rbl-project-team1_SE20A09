package com.sportvenue.scheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
class BookingCompletionSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingCompletionScheduler scheduler;

    private TimeSlot pastSlot;
    private TimeSlot futureSlot;

    @BeforeEach
    void setUp() {
        // Slot đã kết thúc (hôm qua, 08:00–10:00)
        pastSlot = TimeSlot.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        // Slot chưa kết thúc (hôm nay, 22:00–23:59)
        futureSlot = TimeSlot.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(23, 59))
                .build();
    }

    @Test
    @DisplayName("Booking CONFIRMED với endTime đã qua → chuyển sang COMPLETED")
    void completePastBookings_shouldCompleteExpiredBooking() {
        Booking booking = Booking.builder()
                .bookingId(1)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(LocalDate.now().minusDays(1)) // hôm qua
                .slot(pastSlot)
                .build();

        when(bookingRepository.findConfirmedPastPlayTime(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(booking));

        scheduler.completePastBookings();

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.COMPLETED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Booking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(booking);
    }

    @Test
    @DisplayName("Booking CONFIRMED với endTime chưa qua (hôm nay, slot tối) → KHÔNG chuyển COMPLETED")
    void completePastBookings_shouldNotCompleteActiveBooking() {
        Booking booking = Booking.builder()
                .bookingId(2)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(LocalDate.now()) // hôm nay
                .slot(futureSlot)                 // 22:00–23:59, giả sử now < 22:00
                .build();

        when(bookingRepository.findConfirmedPastPlayTime(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(booking));

        scheduler.completePastBookings();

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Booking có slot null → bị bỏ qua, không gây NullPointerException")
    void completePastBookings_shouldSkipBookingWithNullSlot() {
        Booking booking = Booking.builder()
                .bookingId(3)
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(LocalDate.now().minusDays(1))
                .slot(null)
                .build();

        when(bookingRepository.findConfirmedPastPlayTime(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(booking));

        scheduler.completePastBookings();

        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Không có booking nào → saveAll không được gọi")
    void completePastBookings_shouldDoNothingWhenNoCandidates() {
        when(bookingRepository.findConfirmedPastPlayTime(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.completePastBookings();

        verify(bookingRepository, never()).saveAll(any());
    }
}
