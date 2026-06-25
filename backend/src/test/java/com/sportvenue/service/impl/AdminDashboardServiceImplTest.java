package com.sportvenue.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sportvenue.dto.response.AdminDashboardResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getDashboardData_ShouldReturnCorrectData() {
        // Arrange
        when(userRepository.countByRoleName("Customer")).thenReturn(100L);
        when(userRepository.countByRoleName("Owner")).thenReturn(20L);
        when(stadiumRepository.count()).thenReturn(15L);
        when(bookingRepository.count()).thenReturn(50L);
        when(paymentRepository.sumTotalRevenue()).thenReturn(new BigDecimal("1000000"));

        when(bookingRepository.countByBookingStatus(BookingStatus.PENDING)).thenReturn(10L);
        when(bookingRepository.countByBookingStatus(BookingStatus.PENDING_PAYMENT)).thenReturn(2L);
        when(bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED)).thenReturn(20L);
        when(bookingRepository.countByBookingStatus(BookingStatus.CANCELLED)).thenReturn(5L);
        when(bookingRepository.countByBookingStatus(BookingStatus.COMPLETED)).thenReturn(15L);

        when(ownerRepository.countByApprovedStatus(ApprovedStatus.PENDING)).thenReturn(3L);
        when(complaintRepository.countByStatus(ComplaintStatus.OPEN)).thenReturn(2L);

        User customer = User.builder().firstName("John").lastName("Doe").build();
        Stadium stadium = Stadium.builder().stadiumName("Camp Nou").build();
        TimeSlot slot = TimeSlot.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        Booking booking = Booking.builder()
                .bookingId(1)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("200000"))
                .bookingStatus(BookingStatus.COMPLETED)
                .bookingDate(LocalDateTime.of(2026, 6, 20, 10, 0))
                .reservationDate(LocalDate.of(2026, 6, 21))
                .build();

        when(bookingRepository.findTop5ByOrderByBookingDateDesc()).thenReturn(List.of(booking));
        when(bookingRepository.countBookingsByDateRange(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        AdminDashboardResponse response = adminDashboardService.getDashboardData();

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getTotalUsers());
        assertEquals(20L, response.getTotalOwners());
        assertEquals(15L, response.getTotalStadiums());
        assertEquals(50L, response.getTotalBookings());
        assertEquals(new BigDecimal("1000000"), response.getTotalRevenue());

        assertEquals(10L, response.getPendingBookings());
        assertEquals(20L, response.getConfirmedBookings());
        assertEquals(5L, response.getCancelledBookings());
        assertEquals(15L, response.getCompletedBookings());

        assertEquals(3L, response.getPendingOwnerApprovals());
        assertEquals(2L, response.getOpenComplaints());

        assertEquals(1, response.getRecentBookings().size());
        AdminDashboardResponse.RecentBookingDto recentBookingDto = response.getRecentBookings().get(0);
        assertEquals(1, recentBookingDto.getBookingId());
        assertEquals("John Doe", recentBookingDto.getCustomerName());
        assertEquals("Camp Nou", recentBookingDto.getStadiumName());
        assertEquals(new BigDecimal("200000"), recentBookingDto.getTotalPrice());
        assertEquals("COMPLETED", recentBookingDto.getBookingStatus());
        assertEquals("08:00 - 10:00", recentBookingDto.getTimeSlot());
        assertEquals(LocalDate.of(2026, 6, 21), recentBookingDto.getReservationDate());
    }
}
