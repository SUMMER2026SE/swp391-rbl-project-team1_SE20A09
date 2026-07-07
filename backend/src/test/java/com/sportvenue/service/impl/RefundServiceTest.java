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
        slot.setStartTime(java.time.LocalTime.of(18, 0));
        slot.setEndTime(java.time.LocalTime.of(19, 0));

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
    @DisplayName("processRefund: gateway fails -> throws exception and keeps booking intact")
    void processRefund_gatewayFails_keepsBookingIntact() {
        // Arrange
        com.sportvenue.entity.User ownerUser = com.sportvenue.entity.User.builder()
                .userId(1).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder()
                .ownerId(1).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder()
                .stadiumId(10).owner(owner).build();
        
        booking.setStadium(stadium);
        booking.setUser(ownerUser);
        booking.setTotalPrice(new java.math.BigDecimal("150000"));
        booking.setReservationDate(java.time.LocalDate.now().plusDays(7));

        com.sportvenue.entity.Payment originalPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1).booking(booking).amount(new java.math.BigDecimal("150000"))
                .transactionCode("TXN123").paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS).build();

        com.sportvenue.dto.request.RefundRequest request = new com.sportvenue.dto.request.RefundRequest();
        request.setReason("Owner cancelled");

        org.mockito.Mockito.lenient().when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(java.util.Optional.of(ownerUser));
        org.mockito.Mockito.lenient().when(ownerRepository.findByUserUserId(1))
                .thenReturn(java.util.Optional.of(owner));
        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(originalPayment));
        org.mockito.Mockito.lenient().when(paymentRepository.save(org.mockito.ArgumentMatchers.any(com.sportvenue.entity.Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        org.springframework.transaction.support.TransactionTemplate txTemplate = org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class);
        org.springframework.test.util.ReflectionTestUtils.setField(refundService, "transactionTemplate", txTemplate);
        
        org.mockito.Mockito.lenient().when(txTemplate.execute(org.mockito.ArgumentMatchers.any(org.springframework.transaction.support.TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.transaction.support.TransactionCallback callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        com.sportvenue.service.PaymentService paymentService = org.mockito.Mockito.mock(com.sportvenue.service.PaymentService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(refundService, "paymentService", paymentService);

        org.mockito.Mockito.doThrow(new RuntimeException("Gateway error"))
                .when(paymentService).processRefund(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Act & Assert
        com.sportvenue.exception.BadRequestException ex = assertThrows(com.sportvenue.exception.BadRequestException.class, 
                () -> refundService.processRefund(1, request, "owner@example.com"));
        
        assertTrue(ex.getMessage().contains("thất bại"));
        
        // Confirm booking state is untouched
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(SlotStatus.BOOKED, slot.getSlotStatus());
    }
}
