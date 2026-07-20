package com.sportvenue.repository;

import com.sportvenue.entity.WalletTransaction;
import com.sportvenue.entity.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository quản lý giao dịch ví WalletTransaction.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    Page<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(Integer walletId, Pageable pageable);

    List<WalletTransaction> findByBookingBookingIdOrderByCreatedAtAsc(Integer bookingId);

    /**
     * Tổng phí dịch vụ Platform thực thu trong khoảng thời gian (nguồn sự thật cho Fee).
     * Chỉ tính SERVICE_FEE_CREDIT trên Platform Wallet (isPlatform=true).
     * Không có giới hạn ngày → trả về all-time total.
     */
    @Query("""
            SELECT COALESCE(SUM(wt.amount), 0)
            FROM WalletTransaction wt
            WHERE wt.wallet.isPlatform = true
            AND wt.transactionType = :type
            AND (:start IS NULL OR wt.createdAt >= :start)
            AND (:end IS NULL OR wt.createdAt <= :end)
            """)
    BigDecimal sumPlatformFeeByTypeAndDateRange(
            @Param("type") WalletTransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Tổng phí dịch vụ đã bị trừ từ Ví Owner trong khoảng thời gian.
     * SERVICE_FEE_DEBIT là signed-negative trong Owner Wallet → ABS để lấy giá trị dương.
     */
    @Query("""
            SELECT COALESCE(SUM(ABS(wt.amount)), 0)
            FROM WalletTransaction wt
            LEFT JOIN wt.booking b
            WHERE wt.wallet.owner.ownerId = :ownerId
            AND (:stadiumId IS NULL OR b.stadium.stadiumId = :stadiumId)
            AND wt.transactionType = :type
            AND wt.createdAt >= :start
            AND wt.createdAt <= :end
            AND b.bookingStatus IN :statuses
            """)
    BigDecimal sumOwnerFeeByTypeDateRangeAndStatuses(
            @Param("ownerId") Integer ownerId,
            @Param("stadiumId") Integer stadiumId,
            @Param("type") WalletTransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") java.util.List<com.sportvenue.entity.enums.BookingStatus> statuses);
}
