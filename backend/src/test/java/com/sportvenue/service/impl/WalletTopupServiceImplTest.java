package com.sportvenue.service.impl;

import com.sportvenue.config.VNPayConfig;
import com.sportvenue.dto.request.WalletTopupRequest;
import com.sportvenue.dto.response.VnpayIpnResponse;
import com.sportvenue.dto.response.WalletTopupResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.WalletTopup;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.entity.enums.WalletTransactionType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.WalletTopupRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.WalletService;
import com.sportvenue.service.WalletTopupService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletTopupServiceImplTest {

    private static final String HASH_SECRET = "test-secret";

    @Mock private WalletTopupRepository walletTopupRepository;
    @Mock private UserRepository userRepository;
    @Mock private VNPayConfig vnPayConfig;
    @Mock private WalletService walletService;

    @InjectMocks
    private WalletTopupServiceImpl walletTopupService;

    private User customer;
    private WalletTopup topup;

    @BeforeEach
    void setUp() {
        customer = User.builder().userId(30).email("customer@test.com").build();

        topup = WalletTopup.builder()
                .topupId(1)
                .user(customer)
                .amount(new BigDecimal("100000"))
                .transactionCode("TOPUP-30-1721200000000")
                .status(TransactionStatus.PENDING)
                .build();

        lenient().when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
    }

    // ── initiateTopup ───────────────────────────────────────────────────────

    @Test
    void initiateTopup_validAmount_savesTopupAndReturnsPaymentUrl() {
        lenient().when(vnPayConfig.getUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        lenient().when(vnPayConfig.getVersion()).thenReturn("2.1.0");
        lenient().when(vnPayConfig.getCommand()).thenReturn("pay");
        lenient().when(vnPayConfig.getTmnCode()).thenReturn("TMNCODE");
        lenient().when(vnPayConfig.getCurrencyCode()).thenReturn("VND");
        lenient().when(vnPayConfig.getOrderType()).thenReturn("other");
        lenient().when(vnPayConfig.getLocale()).thenReturn("vn");
        lenient().when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:8080/api/v1/payments/vnpay-return");
        when(userRepository.getReferenceById(30)).thenReturn(customer);
        when(walletTopupRepository.save(any(WalletTopup.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPrincipal principal = new UserPrincipal(customer);
        WalletTopupRequest request = new WalletTopupRequest(new BigDecimal("100000"));
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        WalletTopupResponse response = walletTopupService.initiateTopup(principal, request, httpRequest);

        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().startsWith(vnPayConfig.getUrl()));
        assertTrue(response.getPaymentUrl().contains("vnp_SecureHash="));
        verify(walletTopupRepository).save(org.mockito.ArgumentMatchers.argThat(t ->
                t.getAmount().compareTo(new BigDecimal("100000")) == 0
                        && t.getStatus() == TransactionStatus.PENDING
                        && t.getTransactionCode().startsWith(WalletTopupService.TXN_REF_PREFIX)));
    }

    // ── isTopupCallback ──────────────────────────────────────────────────────

    @Test
    void isTopupCallback_topupPrefix_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("vnp_TxnRef", "TOPUP-30-1721200000000");
        assertTrue(walletTopupService.isTopupCallback(request));
    }

    @Test
    void isTopupCallback_bookingTxnRef_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("vnp_TxnRef", "123_1721200000000");
        assertFalse(walletTopupService.isTopupCallback(request));
    }

    // ── handleTopupReturn / handleTopupIpn — core callback xử lý ────────────

    @Test
    void handleTopupReturn_success_creditsWalletAndMarksSuccess() {
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.of(topup));

        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 10_000_000L, "00"));

        WalletTopupService.TopupReturnResult result = walletTopupService.handleTopupReturn(request);

        assertTrue(result.success());
        assertEquals(0, new BigDecimal("100000").compareTo(result.amount()));
        assertEquals(TransactionStatus.SUCCESS, topup.getStatus());
        assertNotNull(topup.getPaidAt());
        verify(walletService).recordCustomerTransaction(
                eq(30), eq(new BigDecimal("100000")), eq(null),
                eq(WalletTransactionType.CUSTOMER_TOPUP_CREDIT), anyString());
    }

    @Test
    void handleTopupReturn_alreadyConfirmed_doesNotCreditAgain() {
        topup.setStatus(TransactionStatus.SUCCESS);
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.of(topup));

        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 10_000_000L, "00"));

        WalletTopupService.TopupReturnResult result = walletTopupService.handleTopupReturn(request);

        assertTrue(result.success());
        verify(walletService, never()).recordCustomerTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void handleTopupReturn_invalidAmount_doesNotCredit() {
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.of(topup));

        // topup.amount = 100000 -> vnp_Amount hợp lệ phải là 10_000_000 (x100) — cố tình gửi sai.
        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 5_000_000L, "00"));

        WalletTopupService.TopupReturnResult result = walletTopupService.handleTopupReturn(request);

        assertFalse(result.success());
        assertEquals("invalid_amount", result.reason());
        verify(walletService, never()).recordCustomerTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void handleTopupReturn_invalidSignature_doesNotTouchRepository() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("vnp_TxnRef", "TOPUP-30-1721200000000");
        request.setParameter("vnp_Amount", "10000000");
        request.setParameter("vnp_ResponseCode", "00");
        request.setParameter("vnp_SecureHash", "clearly-wrong-hash");

        WalletTopupService.TopupReturnResult result = walletTopupService.handleTopupReturn(request);

        assertFalse(result.success());
        assertEquals("invalid_hash", result.reason());
        verify(walletTopupRepository, never()).findByTransactionCodeForUpdate(anyString());
    }

    @Test
    void handleTopupReturn_orderNotFound_throwsBadRequest() {
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 10_000_000L, "00"));

        assertThrows(BadRequestException.class, () -> walletTopupService.handleTopupReturn(request));
    }

    @Test
    void handleTopupReturn_gatewayResponseFailed_marksFailedWithoutCrediting() {
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.of(topup));

        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 10_000_000L, "24"));

        WalletTopupService.TopupReturnResult result = walletTopupService.handleTopupReturn(request);

        assertFalse(result.success());
        assertEquals(TransactionStatus.FAILED, topup.getStatus());
        verify(walletService, never()).recordCustomerTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void handleTopupIpn_success_returnsRspCode00() {
        when(walletTopupRepository.findByTransactionCodeForUpdate("TOPUP-30-1721200000000"))
                .thenReturn(Optional.of(topup));

        MockHttpServletRequest request = buildSignedRequest(baseParams("TOPUP-30-1721200000000", 10_000_000L, "00"));

        VnpayIpnResponse response = walletTopupService.handleTopupIpn(request);

        assertEquals("00", response.getRspCode());
    }

    private Map<String, String> baseParams(String txnRef, long amount, String responseCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_OrderInfo", "Nap tien vao vi #30");
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
