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
import java.time.LocalDate;
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
     * Lọc theo wt.createdAt — dùng cho các query đơn giản theo thời gian ghi sổ.
     * Để lọc theo reservationDate (cùng chuẩn với danh sách booking), dùng sumPlatformFeeByFilters.
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
     * Tổng phí dịch vụ đã bị trừ từ Ví Owner theo b.reservationDate (ngày chơi thực tế).
     * Thống nhất với Gross/Refund và danh sách Owner bookings — tránh lệch khi ngày thanh toán
     * (createdAt) khác ngày chơi (reservationDate).
     * SERVICE_FEE_DEBIT là signed-negative trong Owner Wallet → ABS để lấy giá trị dương.
     */
    @Query("""
            SELECT COALESCE(SUM(ABS(wt.amount)), 0)
            FROM WalletTransaction wt
            JOIN wt.booking b
            JOIN b.stadium s
            WHERE wt.wallet.owner.ownerId = :ownerId
            AND (:stadiumId IS NULL OR b.stadium.stadiumId = :stadiumId)
            AND wt.transactionType = :type
            AND b.reservationDate >= :startDate
            AND b.reservationDate <= :endDate
            AND b.bookingStatus IN :statuses
            """)
    BigDecimal sumOwnerFeeByTypeDateRangeAndStatuses(
            @Param("ownerId") Integer ownerId,
            @Param("stadiumId") Integer stadiumId,
            @Param("type") WalletTransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") java.util.List<com.sportvenue.entity.enums.BookingStatus> statuses);

    /**
     * Tổng Fee Platform (SERVICE_FEE_CREDIT) theo b.reservationDate và toàn bộ Admin filter.
     * Thống nhất cột ngày với danh sách booking và Gross/Refund aggregate —
     * tránh lệch giữa block stats và bảng Admin bookings.
     */
    @Query("""
            SELECT COALESCE(SUM(wt.amount), 0)
            FROM WalletTransaction wt
            JOIN wt.booking b
            JOIN b.stadium s
            WHERE wt.wallet.isPlatform = true
            AND wt.transactionType = :type
            AND b.reservationDate >= :startDate
            AND b.reservationDate <= :endDate
            AND b.bookingStatus IN :bookingStatuses
            AND b.paymentStatus IN :paymentStatuses
            AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId)
            AND (:ownerId IS NULL OR s.owner.ownerId = :ownerId)
            """)
    BigDecimal sumPlatformFeeByFilters(
            @Param("type") WalletTransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("bookingStatuses") java.util.List<com.sportvenue.entity.enums.BookingStatus> bookingStatuses,
            @Param("paymentStatuses") java.util.List<com.sportvenue.entity.enums.PaymentStatus> paymentStatuses,
            @Param("stadiumId") Integer stadiumId,
            @Param("ownerId") Integer ownerId);
}
