package com.sportvenue.service;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OwnerBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private OwnerBookingService ownerBookingService;

    private Booking booking;
    private TimeSlot slot;
    private User user;
    private Stadium stadium;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("0123456789")
                .build();

        SportType sportType = new SportType();
        sportType.setSportName("Football");

        stadium = new Stadium();
        stadium.setStadiumId(1);
        stadium.setStadiumName("Test Stadium");
        stadium.setAddress("Test Address");
        stadium.setSportType(sportType);

        slot = new TimeSlot();
        slot.setSlotId(1);
        slot.setStartTime(LocalDateTime.now().withHour(8).withMinute(0));
        slot.setEndTime(LocalDateTime.now().withHour(9).withMinute(0));
        slot.setSlotStatus(SlotStatus.AVAILABLE);

        booking = new Booking();
        booking.setBookingId(1);
        booking.setUser(user);
        booking.setStadium(stadium);
        booking.setSlot(slot);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setTotalPrice(BigDecimal.valueOf(100000));
        booking.setBookingDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should confirm booking and update slot to BOOKED when slot is AVAILABLE")
    void confirmBooking_Success() throws Exception {
        Method method = OwnerBookingService.class.getDeclaredMethod("confirmBooking", Booking.class);
        method.setAccessible(true);

        method.invoke(ownerBookingService, booking);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(SlotStatus.BOOKED, slot.getSlotStatus());
    }

    @Test
    @DisplayName("Should throw BadRequestException when confirming booking but slot is already BOOKED")
    void confirmBooking_Conflict() throws Exception {
        slot.setSlotStatus(SlotStatus.BOOKED);
        
        Method method = OwnerBookingService.class.getDeclaredMethod("confirmBooking", Booking.class);
        method.setAccessible(true);

        try {
            method.invoke(ownerBookingService, booking);
            fail("Should have thrown BadRequestException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof BadRequestException);
            assertEquals("Khung giờ này đã được đặt bởi một khách hàng khác.", e.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Should release slot when rejecting booking if slot is not BOOKED")
    void rejectBooking_ReleaseSlot() throws Exception {
        Method method = OwnerBookingService.class.getDeclaredMethod("rejectBooking", Booking.class, String.class);
        method.setAccessible(true);

        method.invoke(ownerBookingService, booking, "Reason");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());
    }

    @Test
    @DisplayName("Should NOT release slot when rejecting booking if slot is already BOOKED by another")
    void rejectBooking_KeepBookedStatus() throws Exception {
        slot.setSlotStatus(SlotStatus.BOOKED);
        
        Method method = OwnerBookingService.class.getDeclaredMethod("rejectBooking", Booking.class, String.class);
        method.setAccessible(true);

        method.invoke(ownerBookingService, booking, "Reason");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(SlotStatus.BOOKED, slot.getSlotStatus());
    }
}
