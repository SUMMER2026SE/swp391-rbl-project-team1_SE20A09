package com.sportvenue.service;

import com.sportvenue.config.VNPayConfig;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.service.impl.PaymentServiceImpl;
import com.sportvenue.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String HASH_SECRET = "test-secret";

    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private com.sportvenue.util.AfterCommitExecutor afterCommitExecutor;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private VNPayConfig vnPayConfig;
    @Mock private AdminDashboardService adminDashboardService;
    @Mock private VnpayRefundGateway vnpayRefundGateway;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .paymentId(1)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionCode("VNP123456")
                .amount(new BigDecimal("100000"))
                .paymentStatus(TransactionStatus.PENDING)
                .build();
        lenient().when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
    }

    // ── processRefund / checkRefundStatus: chỉ còn delegate mỏng sang VnpayRefundGateway
    // (logic mock thật đã chuyển sang MockVnpayRefundGatewayTest) ──────────────────────

    @Test
    void processRefund_DelegatesToGateway() {
        BigDecimal refundAmount = new BigDecimal("50000");

        paymentService.processRefund(payment, refundAmount, "Test Refund");

        verify(vnpayRefundGateway).refund(payment, refundAmount, "Test Refund");
    }

    @Test
    void checkRefundStatus_DelegatesToGateway() {
        when(vnpayRefundGateway.queryRefundStatus(payment)).thenReturn(true);

        boolean result = paymentService.checkRefundStatus(payment);

        assertTrue(result);
        verify(vnpayRefundGateway).queryRefundStatus(payment);
    }

    // ── processVnpayCallback: core dùng chung cho VNPay return + IPN ───────────────────

    @Test
    void processVnpayCallback_AlreadyConfirmed_WhenPaymentAlreadySuccess() {
        payment.setPaymentStatus(TransactionStatus.SUCCESS);
        payment.setBooking(Booking.builder().bookingId(1).build());
        when(paymentRepository.findByTransactionCodeForUpdate("VNP123456"))
                .thenReturn(Optional.of(payment));

        MockHttpServletRequest request = buildSignedRequest(baseVnpParams("VNP123456", 10_000_000L));

        PaymentService.CallbackOutcome outcome = paymentService.processVnpayCallback(request);

        assertEquals(PaymentService.CallbackStatus.ALREADY_CONFIRMED, outcome.status());
        assertEquals(1, outcome.bookingId());
    }

    @Test
    void processVnpayCallback_InvalidAmount_WhenAmountMismatchesPaymentAmount() {
        when(paymentRepository.findByTransactionCodeForUpdate("VNP123456"))
                .thenReturn(Optional.of(payment));

        // payment.amount = 100_000 → vnp_Amount hợp lệ phải là 10_000_000 (x100) — cố tình gửi sai.
        MockHttpServletRequest request = buildSignedRequest(baseVnpParams("VNP123456", 5_000_000L));

        PaymentService.CallbackOutcome outcome = paymentService.processVnpayCallback(request);

        assertEquals(PaymentService.CallbackStatus.INVALID_AMOUNT, outcome.status());
    }

    @Test
    void processVnpayCallback_InvalidSignature_WhenChecksumWrong() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("vnp_TxnRef", "VNP123456");
        request.setParameter("vnp_Amount", "10000000");
        request.setParameter("vnp_ResponseCode", "00");
        request.setParameter("vnp_SecureHash", "clearly-wrong-hash");

        PaymentService.CallbackOutcome outcome = paymentService.processVnpayCallback(request);

        assertEquals(PaymentService.CallbackStatus.INVALID_SIGNATURE, outcome.status());
    }

    private Map<String, String> baseVnpParams(String txnRef, long amount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_OrderInfo", "Thanh toan don dat san #1");
        return params;
    }

    private MockHttpServletRequest buildSignedRequest(Map<String, String> params) {
        Map<String, String> signed = new LinkedHashMap<>(params);
        String signingString = VNPayUtil.buildSigningString(signed);
        String hash = VNPayUtil.hmacSHA512(HASH_SECRET, signingString);
        signed.put("vnp_SecureHash", hash);

        MockHttpServletRequest request = new MockHttpServletRequest();
        signed.forEach(request::setParameter);
        return request;
    }
}
