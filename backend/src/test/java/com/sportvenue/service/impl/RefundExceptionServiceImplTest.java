package com.sportvenue.service.impl;

import com.sportvenue.dto.request.AdminDecisionRequest;
import com.sportvenue.dto.request.OwnerReviewRequest;
import com.sportvenue.dto.request.SubmitExceptionRequest;
import com.sportvenue.dto.response.RefundExceptionResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.RefundExceptionStatus;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.entity.enums.WalletTransactionType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.RefundExceptionRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.PaymentService;
import com.sportvenue.service.WalletService;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundExceptionServiceImplTest {

    @Mock private RefundExceptionRepository refundExceptionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private PaymentService paymentService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private WalletService walletService;

    @InjectMocks
    private RefundExceptionServiceImpl service;

    // ─── shared fixtures ───
    private User customer;
    private User ownerUser;
    private Owner owner;
    private Stadium stadium;
    private Booking cancelledBooking;
    private Payment successPayment;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().roleId(1).roleName("Customer").build();
        Role ownerRole   = Role.builder().roleId(2).roleName("Owner").build();

        customer = User.builder().userId(1).email("customer@test.com")
                .firstName("Khach").lastName("Hang").role(customerRole).build();
        ownerUser = User.builder().userId(2).email("owner@test.com")
                .firstName("Chu").lastName("San").role(ownerRole).build();

        owner = Owner.builder().ownerId(1).user(ownerUser).build();
        stadium = Stadium.builder().stadiumId(1).stadiumName("San Test").owner(owner).build();

        cancelledBooking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .bookingStatus(BookingStatus.CANCELLED)
                .paymentStatus(PaymentStatus.UNPAID)
                .bookingDate(LocalDateTime.now().minusHours(1))  // 1h ago — within 72h
                .reservationDate(LocalDate.now().plusDays(1))
                .build();

        successPayment = Payment.builder()
                .paymentId(10)
                .booking(cancelledBooking)
                .amount(new BigDecimal("150000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(TransactionStatus.SUCCESS)
                .transactionCode("TXN001")
                .build();

        // Default TransactionTemplate mock — executes callback immediately
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        lenient().when(refundExceptionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ═══════════════════════════════════════════════════════════
    // submitRequest
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("submitRequest: đúng điều kiện → thành công")
    void submitRequest_validConditions_success() {
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        when(refundExceptionRepository.save(any())).thenAnswer(inv -> {
            RefundExceptionRequest r = inv.getArgument(0);
            r = RefundExceptionRequest.builder()
                    .requestId(1).booking(cancelledBooking).customer(customer)
                    .reason(r.getReason()).status(RefundExceptionStatus.PENDING_OWNER)
                    .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusHours(72))
                    .build();
            return r;
        });

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Tôi bị tai nạn xe máy, không thể đến sân được, có ảnh bằng chứng.");

        RefundExceptionResponse res = service.submitRequest("customer@test.com", req);

        assertNotNull(res);
        assertEquals(RefundExceptionStatus.PENDING_OWNER, res.getStatus());
        verify(notificationService).publishNotificationEvent(
                eq(ownerUser.getUserId()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("submitRequest: đơn đặt cọc (amount < baseCourtPrice) → BadRequestException")
    void submitRequest_depositBookingBlocked_throwsBadRequest() {
        cancelledBooking.setTotalPrice(new BigDecimal("150000"));
        cancelledBooking.setServiceFee(new BigDecimal("10000"));

        Payment depositPayment = Payment.builder()
                .paymentId(10)
                .booking(cancelledBooking)
                .amount(new BigDecimal("45000"))
                .paymentStatus(TransactionStatus.SUCCESS)
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(depositPayment));

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
        assertTrue(ex.getMessage().contains("không áp dụng cho đơn đặt cọc"));
    }

    @Test
    @DisplayName("submitRequest: booking chưa hủy (CONFIRMED) → BadRequestException")
    void submitRequest_bookingNotCancelled_throwsBadRequest() {
        cancelledBooking.setBookingStatus(BookingStatus.CONFIRMED);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
    }

    @Test
    @DisplayName("submitRequest: quá 72h kể từ khi HỦY (paidAt của refund Payment) → BadRequestException")
    void submitRequest_after72Hours_throwsBadRequest() {
        // bookingDate (thời điểm TẠO đơn) vẫn gần — chứng minh check dùng paidAt của refund
        // Payment (thời điểm HỦY), không phải bookingDate.
        Payment zeroRefund = Payment.builder()
                .paymentId(50).amount(BigDecimal.ZERO)
                .paymentStatus(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.now().minusHours(80)) // hủy cách đây 80h — quá hạn
                .build();
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(zeroRefund));

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
    }

    @Test
    @DisplayName("submitRequest: booking TẠO lâu (>72h) nhưng HỦY gần đây (<72h) → vẫn thành công")
    void submitRequest_oldBookingDateButRecentlyCancelled_success() {
        // Đây là kịch bản thực tế phổ biến nhất: đặt sân trước nhiều ngày, hủy sát giờ chơi.
        // bookingDate cũ (>72h) không được phép chặn nếu thời điểm HỦY thật sự còn trong hạn.
        cancelledBooking.setBookingDate(LocalDateTime.now().minusHours(200)); // tạo đơn 200h trước
        Payment zeroRefund = Payment.builder()
                .paymentId(50).amount(BigDecimal.ZERO)
                .paymentStatus(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.now().minusHours(1)) // nhưng mới hủy 1h trước — còn hạn
                .build();
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(zeroRefund));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(false);

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Tôi bị tai nạn xe máy, không thể đến sân được, có ảnh bằng chứng.");

        RefundExceptionResponse res = service.submitRequest("customer@test.com", req);

        assertNotNull(res);
        assertEquals(RefundExceptionStatus.PENDING_OWNER, res.getStatus());
    }

    @Test
    @DisplayName("submitRequest: đang có request PENDING → BadRequestException")
    void submitRequest_existsActiveRequest_throwsBadRequest() {
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(true);

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
    }

    @Test
    @DisplayName("submitRequest: booking đã hoàn ĐỦ 100% trước đó → BadRequestException (double-refund guard)")
    void submitRequest_alreadyFullyRefunded_throwsBadRequest() {
        // originalPayment 150000, đã hoàn đủ -150000 -> hoàn ĐỦ 100%, không còn gì để xin thêm
        Payment existingRefund = Payment.builder()
                .paymentId(99).amount(new BigDecimal("-150000"))
                .paymentStatus(TransactionStatus.SUCCESS).build();
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(existingRefund));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
        assertTrue(ex.getMessage().contains("đã được hoàn tiền toàn bộ"));
    }

    @Test
    @DisplayName("submitRequest: booking chỉ mới hoàn 50% (chưa đủ 100%) → vẫn gửi được (mục 2 QA)")
    void submitRequest_partiallyRefunded50Percent_stillAllowed() {
        // originalPayment 150000, mới hoàn -75000 (50%) -> chưa đủ 100%, vẫn cho xin ngoại lệ
        Payment existingRefund = Payment.builder()
                .paymentId(99).amount(new BigDecimal("-75000"))
                .paymentStatus(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.now().minusHours(1))
                .build();
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(existingRefund));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Tôi bị tai nạn xe máy, không thể đến sân được, có ảnh bằng chứng.");

        RefundExceptionResponse res = service.submitRequest("customer@test.com", req);

        assertNotNull(res);
        assertEquals(RefundExceptionStatus.PENDING_OWNER, res.getStatus());
    }

    @Test
    @DisplayName("submitRequest: booking chưa từng thanh toán qua cổng (hủy cash-tại-sân) → BadRequestException")
    void submitRequest_neverPaid_throwsBadRequest() {
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(cancelledBooking));
        when(refundExceptionRepository.existsByBookingBookingIdAndStatusIn(anyInt(), anyList())).thenReturn(false);
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of());

        SubmitExceptionRequest req = new SubmitExceptionRequest();
        req.setBookingId(100);
        req.setReason("Lý do dài hơn 20 ký tự để qua validation.");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.submitRequest("customer@test.com", req));
        assertTrue(ex.getMessage().contains("chưa từng thanh toán"));
    }

    // ═══════════════════════════════════════════════════════════
    // ownerReview
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("ownerReview: approved=true, refundPercent=100 → APPROVED_OWNER + trigger refund")
    void ownerReview_approved_triggersRefund() {
        RefundExceptionRequest request = buildPendingOwnerRequest();
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p = Payment.builder().paymentId(99).amount(p.getAmount())
                    .paymentStatus(p.getPaymentStatus()).build();
            return p;
        });
        when(paymentRepository.findById(99)).thenReturn(Optional.of(
                Payment.builder().paymentId(99).paymentStatus(TransactionStatus.PENDING).build()));
        when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(cancelledBooking));

        OwnerReviewRequest req = new OwnerReviewRequest();
        req.setApproved(true);
        req.setRefundPercent(100);
        req.setOwnerNote("Tôi thấy lý do hợp lý");

        RefundExceptionResponse res = service.ownerReview("owner@test.com", 1, req);

        assertEquals(RefundExceptionStatus.APPROVED_OWNER, res.getStatus());
        assertEquals(100, res.getRefundPercent());
        verify(paymentService).processRefund(any(), any(), any());
        verify(walletService).recordOwnerTransaction(
                eq(owner.getOwnerId()),
                eq(new BigDecimal("-150000")),
                eq(cancelledBooking.getBookingId()),
                eq(WalletTransactionType.REFUND_DEBIT),
                anyString()
        );
    }

    @Test
    @DisplayName("ownerReview: approved=true nhưng refundPercent=0 → BadRequestException (business rule)")
    void ownerReview_approvedWithZeroPercent_throwsBadRequest() {
        RefundExceptionRequest request = buildPendingOwnerRequest();
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));

        OwnerReviewRequest req = new OwnerReviewRequest();
        req.setApproved(true);
        req.setRefundPercent(0); // không hợp lệ

        assertThrows(BadRequestException.class,
                () -> service.ownerReview("owner@test.com", 1, req));
    }

    @Test
    @DisplayName("ownerReview: Owner sai sân → ForbiddenException")
    void ownerReview_wrongOwner_throwsForbidden() {
        RefundExceptionRequest request = buildPendingOwnerRequest();
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));

        OwnerReviewRequest req = new OwnerReviewRequest();
        req.setApproved(false);

        assertThrows(ForbiddenException.class,
                () -> service.ownerReview("other-owner@test.com", 1, req));
    }

    @Test
    @DisplayName("ownerReview: rejected → REJECTED_OWNER, notify khách")
    void ownerReview_rejected_setsRejectedOwner() {
        RefundExceptionRequest request = buildPendingOwnerRequest();
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(refundExceptionRepository.save(any())).thenReturn(request);

        OwnerReviewRequest req = new OwnerReviewRequest();
        req.setApproved(false);
        req.setOwnerNote("Không có bằng chứng");

        RefundExceptionResponse res = service.ownerReview("owner@test.com", 1, req);

        assertEquals(RefundExceptionStatus.REJECTED_OWNER, res.getStatus());
        assertTrue(res.isCanEscalate());
        verify(paymentService, never()).processRefund(any(), any(), any());
    }

    // ═══════════════════════════════════════════════════════════
    // customerEscalate
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("customerEscalate: status REJECTED_OWNER → PENDING_ADMIN")
    void customerEscalate_rejectedOwner_success() {
        RefundExceptionRequest request = buildRequest(RefundExceptionStatus.REJECTED_OWNER);
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(refundExceptionRepository.save(any())).thenReturn(request);
        when(userRepository.findAllAdmins()).thenReturn(List.of());

        service.customerEscalate("customer@test.com", 1);

        assertEquals(RefundExceptionStatus.PENDING_ADMIN, request.getStatus());
    }

    @Test
    @DisplayName("customerEscalate: status PENDING_OWNER → BadRequestException")
    void customerEscalate_wrongStatus_throwsBadRequest() {
        RefundExceptionRequest request = buildPendingOwnerRequest();
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));

        assertThrows(BadRequestException.class,
                () -> service.customerEscalate("customer@test.com", 1));
    }

    @Test
    @DisplayName("customerEscalate: status APPROVED_OWNER (khách không hài lòng mức đã duyệt) → PENDING_ADMIN")
    void customerEscalate_approvedOwner_success() {
        RefundExceptionRequest request = buildRequest(RefundExceptionStatus.APPROVED_OWNER);
        request.setRefundPercent(50);
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(refundExceptionRepository.save(any())).thenReturn(request);
        when(userRepository.findAllAdmins()).thenReturn(List.of());

        service.customerEscalate("customer@test.com", 1);

        assertEquals(RefundExceptionStatus.PENDING_ADMIN, request.getStatus());
    }

    // ═══════════════════════════════════════════════════════════
    // adminDecide — gateway resilience
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("adminDecide: approve 50% → APPROVED_ADMIN + refund triggered")
    void adminDecide_approved_triggersRefund() {
        RefundExceptionRequest request = buildRequest(RefundExceptionStatus.PENDING_ADMIN);
        request.setRefundPercent(50);
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return Payment.builder().paymentId(88).amount(p.getAmount())
                    .paymentStatus(p.getPaymentStatus()).build();
        });
        when(paymentRepository.findById(88)).thenReturn(Optional.of(
                Payment.builder().paymentId(88).paymentStatus(TransactionStatus.PENDING).build()));
        when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(cancelledBooking));

        AdminDecisionRequest req = new AdminDecisionRequest();
        req.setApproved(true);
        req.setRefundPercent(50);

        RefundExceptionResponse res = service.adminDecide("admin@test.com", 1, req);

        assertEquals(RefundExceptionStatus.APPROVED_ADMIN, res.getStatus());
        verify(paymentService).processRefund(any(), eq(new BigDecimal("75000")), any());
        verify(walletService).recordOwnerTransaction(
                eq(owner.getOwnerId()),
                eq(new BigDecimal("-75000")),
                eq(cancelledBooking.getBookingId()),
                eq(WalletTransactionType.REFUND_DEBIT),
                anyString()
        );
    }

    @Test
    @DisplayName("adminDecide: gateway throw → Payment=FAILED, status giữ APPROVED_ADMIN, throw 400")
    void adminDecide_gatewayFails_paymentFailedStatusPreserved() {
        RefundExceptionRequest request = buildRequest(RefundExceptionStatus.PENDING_ADMIN);
        request.setRefundPercent(100);
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));

        Payment pendingPayment = Payment.builder().paymentId(77)
                .amount(new BigDecimal("-150000")).paymentStatus(TransactionStatus.PENDING).build();
        when(paymentRepository.save(any())).thenReturn(pendingPayment);
        when(paymentRepository.findById(77)).thenReturn(Optional.of(pendingPayment));

        doThrow(new RuntimeException("Gateway timeout")).when(paymentService)
                .processRefund(any(), any(), any());

        AdminDecisionRequest req = new AdminDecisionRequest();
        req.setApproved(true);
        req.setRefundPercent(100);

        // Phải throw BadRequestException về client (không nuốt lỗi)
        assertThrows(BadRequestException.class,
                () -> service.adminDecide("admin@test.com", 1, req));

        // Payment phải được set FAILED (không rollback, không mất dấu vết)
        verify(transactionTemplate, atLeastOnce()).execute(any());
        // Status request giữ APPROVED_ADMIN (không rollback về PENDING_ADMIN)
        assertEquals(RefundExceptionStatus.APPROVED_ADMIN, request.getStatus());
    }

    @Test
    @DisplayName("adminDecide: Owner đã duyệt 50% (đã hoàn thật), Admin duyệt 100% → chỉ hoàn phần CHÊNH LỆCH 50% còn lại")
    void adminDecide_topUpAfterOwnerApproval_onlyRefundsRemainingDifference() {
        RefundExceptionRequest request = buildRequest(RefundExceptionStatus.PENDING_ADMIN);
        request.setRefundPercent(50); // mức Owner từng duyệt trước khi leo thang
        when(refundExceptionRepository.findById(1)).thenReturn(Optional.of(request));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));

        // Owner đã duyệt 50% và đã hoàn thành công 75000 trước đó (còn nằm trong payments table)
        Payment ownerApprovedRefund = Payment.builder()
                .paymentId(90).amount(new BigDecimal("-75000"))
                .paymentStatus(TransactionStatus.SUCCESS).build();
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(ownerApprovedRefund));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return Payment.builder().paymentId(91).amount(p.getAmount())
                    .paymentStatus(p.getPaymentStatus()).build();
        });
        when(paymentRepository.findById(91)).thenReturn(Optional.of(
                Payment.builder().paymentId(91).paymentStatus(TransactionStatus.PENDING).build()));
        when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(cancelledBooking));

        AdminDecisionRequest req = new AdminDecisionRequest();
        req.setApproved(true);
        req.setRefundPercent(100); // Admin duyệt cao hơn mức Owner (50% -> 100%)

        service.adminDecide("admin@test.com", 1, req);

        // Desired = 150000 (100%), đã hoàn 75000 trước đó -> chỉ gọi gateway với phần còn lại 75000
        verify(paymentService).processRefund(any(), eq(new BigDecimal("75000")), any());
        verify(walletService).recordOwnerTransaction(
                eq(owner.getOwnerId()),
                eq(new BigDecimal("-75000")),
                eq(cancelledBooking.getBookingId()),
                eq(WalletTransactionType.REFUND_DEBIT),
                anyString()
        );
    }

    // ═══════════════════════════════════════════════════════════
    // helpers
    // ═══════════════════════════════════════════════════════════

    private RefundExceptionRequest buildPendingOwnerRequest() {
        return buildRequest(RefundExceptionStatus.PENDING_OWNER);
    }

    private RefundExceptionRequest buildRequest(RefundExceptionStatus status) {
        return RefundExceptionRequest.builder()
                .requestId(1)
                .booking(cancelledBooking)
                .customer(customer)
                .reason("Lý do bất khả kháng hợp lệ")
                .status(status)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(71))
                .build();
    }
}
