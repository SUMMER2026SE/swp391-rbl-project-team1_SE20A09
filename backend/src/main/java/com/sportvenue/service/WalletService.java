package com.sportvenue.service;

import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import com.sportvenue.entity.Wallet;
import com.sportvenue.entity.enums.WalletTransactionType;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service quản lý Ví nội bộ (Wallet) và các giao dịch liên quan.
 */
public interface WalletService {

    /**
     * Lấy ví của Owner, tự động tạo mới ví với số dư 0 nếu chưa tồn tại.
     * @param ownerId ID của chủ sân
     * @return Wallet thực thể ví của Owner
     */
    Wallet getOrCreateOwnerWallet(Integer ownerId);

    /**
     * Lấy ví của hệ thống (Platform Wallet).
     * @return Wallet thực thể ví Platform
     */
    Wallet getPlatformWallet();

    /**
     * Ghi nhận giao dịch biến động số dư cho ví của Owner.
     */
    void recordOwnerTransaction(Integer ownerId, BigDecimal signedAmount, Integer bookingId, WalletTransactionType type, String note);

    /**
     * Ghi nhận giao dịch biến động số dư cho ví của Platform.
     */
    void recordPlatformTransaction(BigDecimal signedAmount, Integer bookingId, WalletTransactionType type, String note);

    /**
     * Lấy số dư hiện tại của Owner.
     */
    BigDecimal getOwnerBalance(Integer ownerId);

    /**
     * Lấy số dư hiện tại của Platform.
     */
    BigDecimal getPlatformBalance();

    /**
     * Lấy số dư ví của Owner dựa vào userId.
     */
    WalletBalanceResponse getOwnerWalletBalance(Integer userId);

    /**
     * Lấy danh sách giao dịch ví phân trang của Owner dựa vào userId.
     */
    PageResponse<WalletTransactionResponse> getOwnerWalletTransactions(Integer userId, Pageable pageable);

    /**
     * Lấy lịch sử giao dịch ví của một đơn đặt sân (Owner).
     */
    List<WalletTransactionResponse> getWalletTransactionsByBooking(Integer userId, Integer bookingId);

    /**
     * Lấy số dư ví Platform cho Admin.
     */
    WalletBalanceResponse getPlatformWalletBalance();

    /**
     * Lấy danh sách giao dịch ví Platform phân trang cho Admin.
     */
    PageResponse<WalletTransactionResponse> getPlatformWalletTransactions(Pageable pageable);
}
