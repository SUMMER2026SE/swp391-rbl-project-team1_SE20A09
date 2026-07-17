package com.sportvenue.service.impl;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Wallet;
import com.sportvenue.entity.WalletTransaction;
import com.sportvenue.entity.enums.WalletTransactionType;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.WalletRepository;
import com.sportvenue.repository.WalletTransactionRepository;
import com.sportvenue.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;
import com.sportvenue.entity.Booking;
import com.sportvenue.exception.ForbiddenException;

/**
 * Triển khai dịch vụ quản lý ví WalletService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OwnerRepository ownerRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Wallet getOrCreateOwnerWallet(Integer ownerId) {
        return walletRepository.findByOwnerOwnerId(ownerId)
                .orElseGet(() -> {
                    log.info("Khởi tạo ví mới cho Owner ID: {}", ownerId);
                    Owner owner = ownerRepository.findById(ownerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ sân với ID " + ownerId));
                    try {
                        Wallet newWallet = Wallet.builder()
                                .owner(owner)
                                .isPlatform(false)
                                .balance(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        return walletRepository.saveAndFlush(newWallet);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Ví của Owner ID {} đã được luồng khác tạo đồng thời. Đang truy vấn lại.", ownerId);
                        return walletRepository.findByOwnerOwnerId(ownerId)
                                .orElseThrow(() -> new IllegalStateException("Lỗi tranh chấp đồng thời khi khởi tạo ví Owner", e));
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getPlatformWallet() {
        return walletRepository.findByIsPlatformTrue()
                .orElseGet(() -> {
                    log.info("Không tìm thấy ví Platform, đang khởi tạo mặc định...");
                    Wallet platformWallet = Wallet.builder()
                            .isPlatform(true)
                            .balance(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return walletRepository.save(platformWallet);
                });
    }

    @Override
    @Transactional
    public void recordOwnerTransaction(Integer ownerId, BigDecimal signedAmount, Integer bookingId, WalletTransactionType type, String note) {
        log.info("Ghi nhận giao dịch Owner Wallet - OwnerId: {}, Amount: {}, BookingId: {}, Type: {}", ownerId, signedAmount, bookingId, type);
        
        // Khóa pessimistic lock
        Wallet wallet = walletRepository.findByOwnerIdForUpdate(ownerId)
                .orElseGet(() -> {
                    // Nếu chưa có ví (cho tài khoản seed cũ hoặc tạo trước đó), thực hiện tạo và lấy ví
                    Wallet w = getOrCreateOwnerWallet(ownerId);
                    return walletRepository.findByOwnerIdForUpdate(ownerId)
                            .orElse(w);
                });

        wallet.setBalance(wallet.getBalance().add(signedAmount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(signedAmount)
                .booking(bookingId != null ? bookingRepository.getReferenceById(bookingId) : null)
                .transactionType(type)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void recordPlatformTransaction(BigDecimal signedAmount, Integer bookingId, WalletTransactionType type, String note) {
        log.info("Ghi nhận giao dịch Platform Wallet - Amount: {}, BookingId: {}, Type: {}", signedAmount, bookingId, type);

        // Khóa pessimistic lock ví platform
        Wallet wallet = walletRepository.findPlatformWalletForUpdate()
                .orElseGet(() -> {
                    Wallet platformWallet = Wallet.builder()
                            .isPlatform(true)
                            .balance(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return walletRepository.save(platformWallet);
                });

        wallet.setBalance(wallet.getBalance().add(signedAmount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(signedAmount)
                .booking(bookingId != null ? bookingRepository.getReferenceById(bookingId) : null)
                .transactionType(type)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getOwnerBalance(Integer ownerId) {
        return walletRepository.findByOwnerOwnerId(ownerId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPlatformBalance() {
        return walletRepository.findByIsPlatformTrue()
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getOwnerWalletBalance(Integer userId) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));
        Wallet wallet = getOrCreateOwnerWallet(owner.getOwnerId());
        return new WalletBalanceResponse(wallet.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> getOwnerWalletTransactions(Integer userId, Pageable pageable) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));
        Wallet wallet = getOrCreateOwnerWallet(owner.getOwnerId());
        Page<WalletTransaction> page = walletTransactionRepository.findByWalletWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        List<WalletTransactionResponse> dtoList = page.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getWalletTransactionsByBooking(Integer userId, Integer bookingId) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));
        List<WalletTransaction> list = walletTransactionRepository.findByBookingBookingIdOrderByCreatedAtAsc(bookingId);
        
        if (!list.isEmpty()) {
            Booking booking = list.get(0).getBooking();
            if (booking != null && booking.getStadium() != null) {
                Owner resolvedOwner = booking.getStadium().resolveOwner();
                if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
                    throw new ForbiddenException("Bạn không có quyền xem giao dịch của booking này");
                }
            }
        }
        
        return list.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getPlatformWalletBalance() {
        Wallet wallet = getPlatformWallet();
        return new WalletBalanceResponse(wallet.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> getPlatformWalletTransactions(Pageable pageable) {
        Wallet wallet = getPlatformWallet();
        Page<WalletTransaction> page = walletTransactionRepository.findByWalletWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        List<WalletTransactionResponse> dtoList = page.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, dtoList);
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .amount(tx.getAmount())
                .bookingId(tx.getBooking() != null ? tx.getBooking().getBookingId() : null)
                .note(tx.getNote())
                .transactionType(tx.getTransactionType().name())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
