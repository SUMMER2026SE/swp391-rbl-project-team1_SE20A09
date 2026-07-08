package com.sportvenue.service.impl;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.dto.response.RefundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Booking booking;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        slot = new TimeSlot();
        slot.setSlotStatus(SlotStatus.BOOKED);

        booking = new Booking();
        booking.setBookingId(1);
        booking.setSlot(slot);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Should update booking status and save refund reason into note")
    void updateBookingAndReleaseSlot_WithReason() throws Exception {
        Method method = RefundServiceImpl.class.getDeclaredMethod("updateBookingAndReleaseSlot", Booking.class, String.class);
        method.setAccessible(true);

        String reason = "Customer requested refund";
        method.invoke(refundService, booking, reason);

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals("Lý do hủy hoàn tiền: " + reason, booking.getNote());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());
        
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should update booking status without note if reason is null or blank")
    void updateBookingAndReleaseSlot_EmptyReason() throws Exception {
        Method method = RefundServiceImpl.class.getDeclaredMethod("updateBookingAndReleaseSlot", Booking.class, String.class);
        method.setAccessible(true);

        method.invoke(refundService, booking, "  ");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertNull(booking.getNote());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());
        
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("previewRefundForCustomer: Should refund 100% of the deposit amount instead of total price when cancelled >= 24 hours")
    void previewRefundForCustomer_DepositBooking_ShouldRefundCorrectPercentageOfDeposit() {
        // Arrange
        com.sportvenue.entity.User customer = com.sportvenue.entity.User.builder()
                .userId(1).email("customer@example.com").build();
        booking.setUser(customer);
        booking.setTotalPrice(new java.math.BigDecimal("1000000"));
        booking.setReservationDate(java.time.LocalDate.now().plusDays(2)); // > 24h
        booking.setPaymentStatus(PaymentStatus.DEPOSITED);
        
        com.sportvenue.entity.Stadium stadium = new com.sportvenue.entity.Stadium();
        stadium.setStadiumName("Test Stadium");
        booking.setStadium(stadium);
        
        TimeSlot ts = new TimeSlot();
        ts.setStartTime(java.time.LocalTime.now());
        booking.setSlot(ts);

        com.sportvenue.entity.Payment depositPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1).booking(booking).amount(new java.math.BigDecimal("300000")) // 30% deposit
                .transactionCode("TXN123").paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS).build();

        org.mockito.Mockito.lenient().when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(java.util.Optional.of(customer));
        org.mockito.Mockito.lenient().when(bookingRepository.findById(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(depositPayment));

        // Act
        RefundResponse response = refundService.previewRefundForCustomer(1, "customer@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(0, new java.math.BigDecimal("300000").compareTo(response.getRefundAmount()), 
                "Refund amount should be exactly the deposit amount (300000)");
    }
}
