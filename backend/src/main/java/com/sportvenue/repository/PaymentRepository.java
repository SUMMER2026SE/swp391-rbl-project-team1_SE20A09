package com.sportvenue.repository;

import com.sportvenue.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.repository.projection.VenueRevenueProjection;

/**
 * Repository cho Payment entity.
 * Stub — mở rộng thêm khi implement luồng thanh toán.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

       /** Tìm payment theo booking — dùng để kiểm tra trạng thái thanh toán. */
       Optional<Payment> findByBookingBookingId(Integer bookingId);

       /** Tìm payment theo mã giao dịch — dùng khi xử lý callback từ VNPay/MoMo. */
       Optional<Payment> findByTransactionCode(String transactionCode);

       @Query("SELECT CAST(p.paidAt AS date) as date, SUM(p.amount) as revenue " +
           "FROM Payment p " +
           "JOIN p.booking b " +
           "JOIN b.stadium s " +
           "WHERE s.owner.user.email = :ownerEmail " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.paidAt >= :startDate AND p.paidAt <= :endDate " +
           "GROUP BY CAST(p.paidAt AS date) " +
           "ORDER BY CAST(p.paidAt AS date) ASC")
    List<DailyRevenueProjection> getDailyRevenue(@Param("ownerEmail") String ownerEmail,
                                                 @Param("stadiumId") Integer stadiumId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s.stadiumId as stadiumId, s.stadiumName as stadiumName, " +
           "COUNT(DISTINCT b.bookingId) as totalBookings, SUM(p.amount) as totalRevenue " +
           "FROM Payment p " +
           "JOIN p.booking b " +
           "JOIN b.stadium s " +
           "WHERE s.owner.user.email = :ownerEmail " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.paidAt >= :startDate AND p.paidAt <= :endDate " +
           "GROUP BY s.stadiumId, s.stadiumName " +
           "ORDER BY SUM(p.amount) DESC")
    List<VenueRevenueProjection> getVenueRevenueBreakdown(@Param("ownerEmail") String ownerEmail,
                                                          @Param("stadiumId") Integer stadiumId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.booking b " +
           "JOIN b.stadium s " +
           "WHERE s.owner.user.email = :ownerEmail " +
           "AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.paidAt BETWEEN :startOfMonth AND :endOfMonth")
    java.math.BigDecimal sumCurrentMonthRevenue(
            @Param("ownerEmail") String ownerEmail,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId AND p.amount < 0")
    Optional<Payment> findRefundPaymentByBookingId(@Param("bookingId") Integer bookingId);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId IN :bookingIds AND p.amount < 0")
    List<Payment> findRefundPaymentsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS")
    java.math.BigDecimal sumTotalRevenue();
}

