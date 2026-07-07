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
    @DisplayName("Should return 100% refund for OWNER_FAULT with proof")
    void calculateRefund_OwnerFault_Success() throws Exception {
        Method method = RefundServiceImpl.class.getDeclaredMethod("calculateRefund", Booking.class, com.sportvenue.entity.enums.RefundReasonType.class, String.class);
        method.setAccessible(true);

        booking.setTotalPrice(new java.math.BigDecimal("100000"));

        Object result = method.invoke(refundService, booking, com.sportvenue.entity.enums.RefundReasonType.OWNER_FAULT, "https://proof.url/img.jpg");
        
        Method getPercentage = result.getClass().getMethod("getPercentage");
        Method getAmount = result.getClass().getMethod("getAmount");
        
        assertEquals(100, getPercentage.invoke(result));
        assertEquals(new java.math.BigDecimal("100000"), getAmount.invoke(result));
    }

    @Test
    @DisplayName("Should throw BadRequestException for OWNER_FAULT without proof")
    void calculateRefund_OwnerFault_MissingProof() throws Exception {
        Method method = RefundServiceImpl.class.getDeclaredMethod("calculateRefund", Booking.class, com.sportvenue.entity.enums.RefundReasonType.class, String.class);
        method.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(refundService, booking, com.sportvenue.entity.enums.RefundReasonType.OWNER_FAULT, "  ");
        });
        
        assertTrue(exception.getCause() instanceof com.sportvenue.exception.BadRequestException);
    }
}
