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
import com.sportvenue.service.PaymentService;
import com.sportvenue.dto.response.RefundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Booking booking;
    private TimeSlot slot;
    private com.sportvenue.entity.User ownerUser;
    private com.sportvenue.entity.Owner owner;
    private com.sportvenue.entity.Stadium stadium;

    @BeforeEach
    void setUp() {
        slot = new TimeSlot();
        slot.setSlotStatus(SlotStatus.BOOKED);
        slot.setStartTime(java.time.LocalTime.of(18, 0));
        slot.setEndTime(java.time.LocalTime.of(19, 0));

        ownerUser = com.sportvenue.entity.User.builder()
                .userId(1).email("owner@example.com").build();
        owner = com.sportvenue.entity.Owner.builder()
                .ownerId(1).user(ownerUser).build();
        stadium = com.sportvenue.entity.Stadium.builder()
                .stadiumId(10).owner(owner).build();

        booking = new Booking();
        booking.setBookingId(1);
        booking.setSlot(slot);
        booking.setStadium(stadium);
        booking.setUser(ownerUser);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        // Mock TransactionTemplate to execute callbacks immediately
        org.springframework.transaction.support.TransactionTemplate txTemplate = org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class);
        org.springframework.test.util.ReflectionTestUtils.setField(refundService, "transactionTemplate", txTemplate);
        org.mockito.Mockito.lenient().when(txTemplate.execute(org.mockito.ArgumentMatchers.any(org.springframework.transaction.support.TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.transaction.support.TransactionCallback callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    @Test
    @DisplayName("processRefund: Should update booking status and save refund reason into note when refund is 0% (cancelled < 12 hours)")
    void processRefund_ZeroRefund_WithReason_UpdatesBookingAndReleasesSlot() {
        // Arrange
        booking.setReservationDate(java.time.LocalDate.now());
        // Set slot start time to 2 hours from now to get < 12 hours difference
        java.time.LocalTime startTime = java.time.LocalTime.now().plusHours(2);
        slot.setStartTime(startTime);
        
        com.sportvenue.entity.Payment originalPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1).booking(booking).amount(new java.math.BigDecimal("150000"))
                .transactionCode("TXN123").paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS).build();

        com.sportvenue.dto.request.RefundRequest request = new com.sportvenue.dto.request.RefundRequest();
        request.setReason("Late cancellation");

        org.mockito.Mockito.lenient().when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(java.util.Optional.of(ownerUser));
        org.mockito.Mockito.lenient().when(ownerRepository.findByUserUserId(1))
                .thenReturn(java.util.Optional.of(owner));
        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findById(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(originalPayment));

        // Act
        refundService.processRefund(1, request, "owner@example.com");

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals("Lý do hủy hoàn tiền: Late cancellation", booking.getNote());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());

        verify(bookingRepository).save(booking);
        verify(timeSlotRepository).save(slot);
    }

    @Test
    @DisplayName("processRefund: Should update booking status without note if reason is null or blank when refund is 0% (cancelled < 12 hours)")
    void processRefund_ZeroRefund_EmptyReason_UpdatesBookingAndReleasesSlot() {
        // Arrange
        booking.setReservationDate(java.time.LocalDate.now());
        java.time.LocalTime startTime = java.time.LocalTime.now().plusHours(2);
        slot.setStartTime(startTime);
        
        com.sportvenue.entity.Payment originalPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1).booking(booking).amount(new java.math.BigDecimal("150000"))
                .transactionCode("TXN123").paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS).build();

        com.sportvenue.dto.request.RefundRequest request = new com.sportvenue.dto.request.RefundRequest();
        request.setReason("  ");

        org.mockito.Mockito.lenient().when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(java.util.Optional.of(ownerUser));
        org.mockito.Mockito.lenient().when(ownerRepository.findByUserUserId(1))
                .thenReturn(java.util.Optional.of(owner));
        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findById(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(originalPayment));

        // Act
        refundService.processRefund(1, request, "owner@example.com");

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertNull(booking.getNote());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());

        verify(bookingRepository).save(booking);
        verify(timeSlotRepository).save(slot);
    }

    @Test
    @DisplayName("previewRefundForCustomer: Should refund 100% of the deposit amount instead of total price when cancelled >= 24 hours")
    void previewRefundForCustomer_DepositBooking_ShouldRefundCorrectPercentageOfDeposit() {
        // Arrange
        com.sportvenue.entity.User customer = com.sportvenue.entity.User.builder()
                .userId(2).email("customer@example.com").build();
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
        org.mockito.Mockito.when(bookingRepository.findById(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(depositPayment));

        // Act
        RefundResponse response = refundService.previewRefundForCustomer(1, "customer@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(0, new java.math.BigDecimal("300000").compareTo(response.getRefundAmount()), 
                "Refund amount should be exactly the deposit amount (300000)");
    }

    @Test
    @DisplayName("processRefund: gateway fails -> throws exception and keeps booking intact")
    void processRefund_gatewayFails_keepsBookingIntact() {
        // Arrange
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
        org.mockito.Mockito.lenient().when(bookingRepository.findById(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(1))
                .thenReturn(java.util.List.of(originalPayment));
        org.mockito.Mockito.lenient().when(paymentRepository.save(org.mockito.ArgumentMatchers.any(com.sportvenue.entity.Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

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

    @Test
    @DisplayName("processRefund: refund already pending -> throws BadRequestException")
    void processRefund_refundAlreadyPending_throwsBadRequest() {
        // Arrange
        booking.setTotalPrice(new java.math.BigDecimal("150000"));
        booking.setReservationDate(java.time.LocalDate.now().plusDays(7));

        com.sportvenue.dto.request.RefundRequest request = new com.sportvenue.dto.request.RefundRequest();
        request.setReason("Owner cancelled");

        com.sportvenue.entity.Payment pendingRefund = com.sportvenue.entity.Payment.builder()
                .paymentId(2)
                .booking(booking)
                .amount(new java.math.BigDecimal("-150000"))
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.PENDING)
                .build();

        org.mockito.Mockito.lenient().when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(java.util.Optional.of(ownerUser));
        org.mockito.Mockito.lenient().when(ownerRepository.findByUserUserId(1))
                .thenReturn(java.util.Optional.of(owner));
        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(1))
                .thenReturn(java.util.Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findRefundPaymentByBookingId(1))
                .thenReturn(java.util.Optional.of(pendingRefund));

        // Act & Assert
        com.sportvenue.exception.BadRequestException ex = assertThrows(com.sportvenue.exception.BadRequestException.class, 
                () -> refundService.processRefund(1, request, "owner@example.com"));
        
        assertTrue(ex.getMessage().contains("đang được xử lý hoặc đã thành công"));
    }
}
