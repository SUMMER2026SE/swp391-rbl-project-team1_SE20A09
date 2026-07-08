package com.sportvenue.service.impl;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.RefundResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.RefundReasonType;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Booking booking;
    private TimeSlot slot;
    private User ownerUser;
    private Owner owner;
    private Stadium stadium;
    private Payment originalPayment;

    @BeforeEach
    void setUp() {
        // Mock TransactionTemplate to execute callbacks immediately
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        slot = new TimeSlot();
        slot.setSlotStatus(SlotStatus.BOOKED);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 0));

        ownerUser = User.builder()
                .userId(1)
                .email("owner@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        owner = Owner.builder()
                .ownerId(1)
                .user(ownerUser)
                .build();

        stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .stadiumName("San Bong Thu Duc")
                .build();

        booking = new Booking();
        booking.setBookingId(1);
        booking.setSlot(slot);
        booking.setStadium(stadium);
        booking.setUser(ownerUser);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setTotalPrice(new BigDecimal("150000"));

        originalPayment = Payment.builder()
                .paymentId(100)
                .amount(new BigDecimal("150000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionCode("TXN123")
                .paymentStatus(TransactionStatus.SUCCESS)
                .build();
    }

    @Test
    @DisplayName("previewRefund: OWNER_FAULT should return 100% refund regardless of time")
    void previewRefund_OwnerFault_Returns100Percent() {
        // Arrange: Booking is today (too close for normal 100% customer refund)
        booking.setReservationDate(LocalDate.now());
        slot.setStartTime(LocalTime.now().plusHours(2));

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act
        RefundResponse response = refundService.previewRefund(1, RefundReasonType.OWNER_FAULT, "owner@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(new BigDecimal("150000"), response.getRefundAmount());
    }

    @Test
    @DisplayName("previewRefund: CUSTOMER_REQUEST >= 24 hours should return 100% refund")
    void previewRefund_CustomerRequest_MoreThan24Hours_Returns100Percent() {
        // Arrange: 2 days in advance
        booking.setReservationDate(LocalDate.now().plusDays(2));

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act
        RefundResponse response = refundService.previewRefund(1, RefundReasonType.CUSTOMER_REQUEST, "owner@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(new BigDecimal("150000"), response.getRefundAmount());
    }

    @Test
    @DisplayName("previewRefund: CUSTOMER_REQUEST between 12 and 24 hours should return 50% refund")
    void previewRefund_CustomerRequest_Between12And24Hours_Returns50Percent() {
        // Arrange: 15 hours in advance
        LocalDateTime target = LocalDateTime.now().plusHours(15);
        booking.setReservationDate(target.toLocalDate());
        slot.setStartTime(target.toLocalTime());

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act
        RefundResponse response = refundService.previewRefund(1, RefundReasonType.CUSTOMER_REQUEST, "owner@example.com");

        // Assert
        assertEquals(50, response.getRefundPercentage());
        assertEquals(new BigDecimal("75000.0"), response.getRefundAmount());
    }

    @Test
    @DisplayName("previewRefund: CUSTOMER_REQUEST < 12 hours should return 0% refund")
    void previewRefund_CustomerRequest_LessThan12Hours_Returns0Percent() {
        // Arrange: 2 hours in advance
        LocalDateTime target = LocalDateTime.now().plusHours(2);
        booking.setReservationDate(target.toLocalDate());
        slot.setStartTime(target.toLocalTime());

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act
        RefundResponse response = refundService.previewRefund(1, RefundReasonType.CUSTOMER_REQUEST, "owner@example.com");

        // Assert
        assertEquals(0, response.getRefundPercentage());
        assertEquals(BigDecimal.ZERO, response.getRefundAmount());
    }

    @Test
    @DisplayName("processRefund: OWNER_FAULT with valid proof should succeed")
    void processRefund_OwnerFault_WithProof_Succeeds() {
        // Arrange
        booking.setReservationDate(LocalDate.now());
        slot.setStartTime(LocalTime.now().plusHours(2));

        RefundRequest request = new RefundRequest();
        request.setReason("San ngap nuoc");
        request.setReasonType(RefundReasonType.OWNER_FAULT);
        request.setProofUrl("https://proof.url/flood.jpg");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(200);
            }
            return p;
        });
        when(paymentRepository.findById(200)).thenAnswer(invocation -> {
            Payment p = Payment.builder()
                    .paymentId(200)
                    .amount(new BigDecimal("-150000"))
                    .build();
            return Optional.of(p);
        });

        // Act
        RefundResponse response = refundService.processRefund(1, request, "owner@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(new BigDecimal("150000"), response.getRefundAmount());
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals("Lý do hủy hoàn tiền: San ngap nuoc", booking.getNote());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());

        verify(bookingRepository, atLeastOnce()).save(booking);
        verify(timeSlotRepository, atLeastOnce()).save(slot);
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processRefund: OWNER_FAULT without proof should throw BadRequestException")
    void processRefund_OwnerFault_WithoutProof_ThrowsBadRequestException() {
        // Arrange
        RefundRequest request = new RefundRequest();
        request.setReason("San ngap nuoc");
        request.setReasonType(RefundReasonType.OWNER_FAULT);
        request.setProofUrl("   "); // blank proof

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            refundService.processRefund(1, request, "owner@example.com");
        });
        assertEquals("Bắt buộc phải cung cấp bằng chứng (ảnh/mô tả) khi lỗi do chủ sân", exception.getMessage());
    }

    @Test
    @DisplayName("processRefund: CUSTOMER_REQUEST with 0% refund should not save negative payment")
    void processRefund_CustomerRequest_ZeroRefund_DoesNotSavePayment() {
        // Arrange: 2 hours in advance
        LocalDateTime target = LocalDateTime.now().plusHours(2);
        booking.setReservationDate(target.toLocalDate());
        slot.setStartTime(target.toLocalTime());

        RefundRequest request = new RefundRequest();
        request.setReason("Customer changed mind");
        request.setReasonType(RefundReasonType.CUSTOMER_REQUEST);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

        // Act
        RefundResponse response = refundService.processRefund(1, request, "owner@example.com");

        // Assert
        assertEquals(0, response.getRefundPercentage());
        assertEquals(BigDecimal.ZERO, response.getRefundAmount());
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());

        verify(bookingRepository).save(booking);
        verify(timeSlotRepository).save(slot);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processRefund: booking belonging to another owner should throw ForbiddenException")
    void processRefund_WrongOwner_ThrowsForbiddenException() {
        // Arrange: Resolved owner id is 99 (different from owner ID 1)
        Owner otherOwner = Owner.builder().ownerId(99).user(ownerUser).build();
        stadium.setOwner(otherOwner); // Change stadium owner

        RefundRequest request = new RefundRequest();
        request.setReason("Some reason");
        request.setReasonType(RefundReasonType.CUSTOMER_REQUEST);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            refundService.processRefund(1, request, "owner@example.com");
        });
        assertEquals("Bạn không có quyền quản lý đơn đặt sân này!", exception.getMessage());
    }

    @Test
    @DisplayName("processRefund: Should update booking status and save refund reason into note when refund is 0% (cancelled < 12 hours)")
    void processRefund_ZeroRefund_WithReason_UpdatesBookingAndReleasesSlot() {
        // Arrange
        booking.setReservationDate(LocalDate.now());
        // Set slot start time to 2 hours from now to get < 12 hours difference
        LocalTime startTime = LocalTime.now().plusHours(2);
        slot.setStartTime(startTime);

        RefundRequest request = new RefundRequest();
        request.setReason("Late cancellation");
        request.setReasonType(RefundReasonType.CUSTOMER_REQUEST);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

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
        booking.setReservationDate(LocalDate.now());
        LocalTime startTime = LocalTime.now().plusHours(2);
        slot.setStartTime(startTime);

        RefundRequest request = new RefundRequest();
        request.setReason("  ");
        request.setReasonType(RefundReasonType.CUSTOMER_REQUEST);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));

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
        User customer = User.builder()
                .userId(2).email("customer@example.com").build();
        booking.setUser(customer);
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setReservationDate(LocalDate.now().plusDays(2)); // > 24h
        booking.setPaymentStatus(PaymentStatus.DEPOSITED);
        
        Stadium stadiumMock = new Stadium();
        stadiumMock.setStadiumName("Test Stadium");
        booking.setStadium(stadiumMock);
        
        TimeSlot ts = new TimeSlot();
        ts.setStartTime(LocalTime.now());
        booking.setSlot(ts);

        Payment depositPayment = Payment.builder()
                .paymentId(1).booking(booking).amount(new BigDecimal("300000")) // 30% deposit
                .transactionCode("TXN123").paymentStatus(TransactionStatus.SUCCESS).build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(depositPayment));

        // Act
        RefundResponse response = refundService.previewRefundForCustomer(1, "customer@example.com");

        // Assert
        assertEquals(100, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("300000").compareTo(response.getRefundAmount()), 
                "Refund amount should be exactly the deposit amount (300000)");
    }

    @Test
    @DisplayName("processRefund: gateway fails -> throws exception and keeps booking intact")
    void processRefund_gatewayFails_keepsBookingIntact() {
        // Arrange
        booking.setTotalPrice(new BigDecimal("150000"));
        booking.setReservationDate(LocalDate.now().plusDays(7));

        RefundRequest request = new RefundRequest();
        request.setReason("Owner cancelled");
        request.setReasonType(RefundReasonType.OWNER_FAULT);
        request.setProofUrl("https://proof.url/proof.jpg");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(1)).thenReturn(List.of(originalPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(200);
            }
            return p;
        });
        when(paymentRepository.findById(200)).thenAnswer(inv -> {
            Payment p = Payment.builder()
                    .paymentId(200)
                    .amount(new BigDecimal("-150000"))
                    .build();
            return Optional.of(p);
        });

        doThrow(new RuntimeException("Gateway error"))
                .when(paymentService).processRefund(any(), any(), any());

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
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
        booking.setTotalPrice(new BigDecimal("150000"));
        booking.setReservationDate(LocalDate.now().plusDays(7));

        RefundRequest request = new RefundRequest();
        request.setReason("Owner cancelled");
        request.setReasonType(RefundReasonType.CUSTOMER_REQUEST);

        Payment pendingRefund = Payment.builder()
                .paymentId(2)
                .booking(booking)
                .amount(new BigDecimal("-150000"))
                .paymentStatus(TransactionStatus.PENDING)
                .build();

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(booking));
        when(paymentRepository.findRefundPaymentByBookingId(1)).thenReturn(Optional.of(pendingRefund));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> refundService.processRefund(1, request, "owner@example.com"));
        
        assertTrue(ex.getMessage().contains("đang được xử lý hoặc đã thành công"));
    }
}
