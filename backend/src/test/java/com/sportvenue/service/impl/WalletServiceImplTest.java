package com.sportvenue.service.impl;

import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.User;
import com.sportvenue.entity.Wallet;
import com.sportvenue.entity.WalletTransaction;
import com.sportvenue.entity.enums.WalletTransactionType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.WalletRepository;
import com.sportvenue.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Owner owner;
    private Wallet ownerWallet;
    private Wallet platformWallet;
    private User customerUser;
    private Wallet customerWallet;

    @BeforeEach
    void setUp() {
        owner = Owner.builder()
                .ownerId(10)
                .businessName("Test Complex")
                .build();

        ownerWallet = Wallet.builder()
                .walletId(1)
                .owner(owner)
                .isPlatform(false)
                .balance(new BigDecimal("50000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        platformWallet = Wallet.builder()
                .walletId(2)
                .owner(null)
                .isPlatform(true)
                .balance(new BigDecimal("10000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerUser = User.builder()
                .userId(30)
                .email("customer@test.com")
                .firstName("Khach")
                .lastName("Hang")
                .build();

        customerWallet = Wallet.builder()
                .walletId(3)
                .user(customerUser)
                .isPlatform(false)
                .balance(new BigDecimal("200000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getOrCreateOwnerWallet_existingWallet_returnsWallet() {
        when(walletRepository.findByOwnerOwnerId(10)).thenReturn(Optional.of(ownerWallet));

        Wallet result = walletService.getOrCreateOwnerWallet(10);

        assertNotNull(result);
        assertEquals(1, result.getWalletId());
        assertEquals(new BigDecimal("50000"), result.getBalance());
        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void getOrCreateOwnerWallet_newWallet_createsAndReturnsWallet() {
        when(walletRepository.findByOwnerOwnerId(10)).thenReturn(Optional.empty());
        when(ownerRepository.findById(10)).thenReturn(Optional.of(owner));
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        Wallet result = walletService.getOrCreateOwnerWallet(10);

        assertNotNull(result);
        assertFalse(result.isPlatform());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).saveAndFlush(any(Wallet.class));
    }

    @Test
    void recordOwnerTransaction_creditsWallet() {
        when(walletRepository.findByOwnerIdForUpdate(10)).thenReturn(Optional.of(ownerWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.recordOwnerTransaction(10, new BigDecimal("15000"), null, WalletTransactionType.BOOKING_CREDIT, "Test credit");

        assertEquals(new BigDecimal("65000"), ownerWallet.getBalance());
        verify(walletRepository).save(ownerWallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void recordPlatformTransaction_debitsWallet() {
        when(walletRepository.findPlatformWalletForUpdate()).thenReturn(Optional.of(platformWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.recordPlatformTransaction(new BigDecimal("-2000"), null, WalletTransactionType.REFUND_FEE_DEBIT, "Test refund fee");

        assertEquals(new BigDecimal("8000"), platformWallet.getBalance());
        verify(walletRepository).save(platformWallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void getOwnerWalletBalance_returnsBalanceResponse() {
        when(ownerRepository.findByUserUserId(20)).thenReturn(Optional.of(owner));
        when(walletRepository.findByOwnerOwnerId(10)).thenReturn(Optional.of(ownerWallet));

        WalletBalanceResponse response = walletService.getOwnerWalletBalance(20);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000"), response.getBalance());
    }

    @Test
    void getOwnerWalletTransactions_returnsPagedTransactions() {
        Pageable pageable = PageRequest.of(0, 5);
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(100)
                .wallet(ownerWallet)
                .amount(new BigDecimal("50000"))
                .transactionType(WalletTransactionType.BOOKING_CREDIT)
                .createdAt(LocalDateTime.now())
                .note("Transaction note")
                .build();

        Page<WalletTransaction> page = new PageImpl<>(Collections.singletonList(transaction), pageable, 1);

        when(ownerRepository.findByUserUserId(20)).thenReturn(Optional.of(owner));
        when(walletRepository.findByOwnerOwnerId(10)).thenReturn(Optional.of(ownerWallet));
        when(walletTransactionRepository.findByWalletWalletIdOrderByCreatedAtDesc(1, pageable)).thenReturn(page);

        PageResponse<WalletTransactionResponse> response = walletService.getOwnerWalletTransactions(20, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("BOOKING_CREDIT", response.getContent().get(0).getTransactionType());
        assertEquals("Transaction note", response.getContent().get(0).getNote());
    }

    @Test
    void getPlatformWallet_returnsPlatformWallet() {
        when(walletRepository.findByIsPlatformTrue()).thenReturn(Optional.of(platformWallet));
        Wallet result = walletService.getPlatformWallet();
        assertNotNull(result);
        assertTrue(result.isPlatform());
        assertEquals(new BigDecimal("10000"), result.getBalance());
    }

    @Test
    void getPlatformBalance_returnsBalance() {
        when(walletRepository.findByIsPlatformTrue()).thenReturn(Optional.of(platformWallet));
        BigDecimal balance = walletService.getPlatformBalance();
        assertEquals(new BigDecimal("10000"), balance);
    }

    // ═══════════════════════════════════════════════════════════
    // Customer wallet
    // ═══════════════════════════════════════════════════════════

    @Test
    void getOrCreateCustomerWallet_existingWallet_returnsWallet() {
        when(walletRepository.findByUserUserId(30)).thenReturn(Optional.of(customerWallet));

        Wallet result = walletService.getOrCreateCustomerWallet(30);

        assertNotNull(result);
        assertEquals(3, result.getWalletId());
        assertEquals(new BigDecimal("200000"), result.getBalance());
        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void getOrCreateCustomerWallet_newWallet_createsAndReturnsWallet() {
        when(walletRepository.findByUserUserId(30)).thenReturn(Optional.empty());
        when(userRepository.findById(30)).thenReturn(Optional.of(customerUser));
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        Wallet result = walletService.getOrCreateCustomerWallet(30);

        assertNotNull(result);
        assertFalse(result.isPlatform());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).saveAndFlush(any(Wallet.class));
    }

    @Test
    void recordCustomerTransaction_creditsWallet() {
        when(walletRepository.findByUserIdForUpdate(30)).thenReturn(Optional.of(customerWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.recordCustomerTransaction(30, new BigDecimal("50000"), null,
                WalletTransactionType.CUSTOMER_TOPUP_CREDIT, "Nạp tiền vào ví");

        assertEquals(new BigDecimal("250000"), customerWallet.getBalance());
        verify(walletRepository).save(customerWallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void getCustomerWalletBalance_returnsBalanceResponse() {
        when(walletRepository.findByUserUserId(30)).thenReturn(Optional.of(customerWallet));

        WalletBalanceResponse response = walletService.getCustomerWalletBalance(30);

        assertNotNull(response);
        assertEquals(new BigDecimal("200000"), response.getBalance());
    }

    @Test
    void debitCustomerWalletForPayment_sufficientBalance_debitsWallet() {
        when(walletRepository.findByUserIdForUpdate(30)).thenReturn(Optional.of(customerWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.debitCustomerWalletForPayment(30, new BigDecimal("150000"), 500, "Thanh toán đơn #500 bằng Ví");

        assertEquals(new BigDecimal("50000"), customerWallet.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getTransactionType() == WalletTransactionType.CUSTOMER_PAYMENT_DEBIT
                        && tx.getAmount().compareTo(new BigDecimal("-150000")) == 0));
    }

    @Test
    void debitCustomerWalletForPayment_insufficientBalance_throwsBadRequestAndDoesNotDebit() {
        when(walletRepository.findByUserIdForUpdate(30)).thenReturn(Optional.of(customerWallet));

        assertThrows(BadRequestException.class, () ->
                walletService.debitCustomerWalletForPayment(30, new BigDecimal("250000"), 500, "Thanh toán đơn #500 bằng Ví"));

        assertEquals(new BigDecimal("200000"), customerWallet.getBalance());
        verify(walletTransactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
