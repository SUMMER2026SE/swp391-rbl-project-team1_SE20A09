package com.sportvenue.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.PaymentMethod;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.repository.projection.VenueRevenueProjection;

/**
 * Repository cho Payment entity.
 * Stub — mở rộng thêm khi implement luồng thanh toán.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

       /** Tìm payment SUCCESS mới nhất theo booking — an toàn khi có nhiều payment rows (retry). */
       @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS AND p.amount > 0 ORDER BY p.paidAt DESC")
       List<Payment> findSuccessPaymentsByBookingId(@Param("bookingId") Integer bookingId);

       /** Tìm payment theo mã giao dịch — dùng khi xử lý callback từ VNPay/MoMo. */
       Optional<Payment> findByTransactionCode(String transactionCode);

       /**
        * Tìm payment theo mã giao dịch kèm Pessimistic Write Lock — dùng cho VNPay return VÀ IPN,
        * tránh race condition khi cả 2 callback cùng cố cập nhật 1 Payment row gần như đồng thời.
        */
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT p FROM Payment p WHERE p.transactionCode = :txnRef")
       Optional<Payment> findByTransactionCodeForUpdate(@Param("txnRef") String txnRef);

       /**
        * Tìm payment CASH đang chờ xác nhận (PENDING) mới nhất của 1 booking — dùng khi Owner xác
        * nhận đã thu tiền mặt. Dùng {@code @Query} tường minh thay vì derived method name có dấu
        * gạch dưới ({@code Booking_BookingId}) — vi phạm quy tắc đặt tên method của Checkstyle.
        */
       @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId "
               + "AND p.paymentMethod = :paymentMethod AND p.paymentStatus = :paymentStatus "
               + "ORDER BY p.paymentId DESC")
       List<Payment> findCashPaymentsByBookingIdAndMethodAndStatus(
               @Param("bookingId") Integer bookingId,
               @Param("paymentMethod") PaymentMethod paymentMethod,
               @Param("paymentStatus") TransactionStatus paymentStatus);

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

    /**
     * Tìm TẤT CẢ payment "hoàn tiền" của 1 booking, mới nhất trước — caller lấy phần tử đầu qua
     * {@code .stream().findFirst()}. Dùng {@code <= 0} (không phải {@code < 0}) vì hủy <12h trước
     * giờ chơi hợp lệ trả về refund 0đ (docs/qa_findings_refactor_plan.md mục 1.3); payment gốc
     * (tiền khách trả) luôn dương nên không lo trùng với payment gốc.
     *
     * <p>Trả về List thay vì {@code Optional<Payment>} vì 1 booking có thể có NHIỀU payment hoàn
     * tiền theo thời gian (vd: payment hoàn 0đ lúc hủy ban đầu + payment hoàn bổ sung từ luồng
     * "Yêu cầu ngoại lệ hoàn tiền" được duyệt sau đó) — dùng {@code Optional} trực tiếp ở đây sẽ
     * ném {@code NonUniqueResultException} khi có ≥2 dòng khớp.</p>
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId AND p.amount <= 0 ORDER BY p.paidAt DESC")
    List<Payment> findRefundPaymentByBookingId(@Param("bookingId") Integer bookingId);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId IN :bookingIds AND p.amount <= 0")
    List<Payment> findRefundPaymentsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    /**
     * Tính gross revenue: chỉ tính các payment thành công với amount > 0.
     * Refund (amount < 0) bị loại trừ vì UC-ADM-01 yêu cầu gross revenue.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount > 0")
    java.math.BigDecimal sumTotalRevenue();

    /**
     * UC-ADM-01: Tổng gross revenue trong khoảng ngày (paidAt) — dùng khi filter theo date range.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount > 0 " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumTotalRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.PENDING AND p.amount < 0 AND p.paidAt <= :threshold")
    List<Payment> findPendingRefundsOlderThan(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId IN :bookingIds AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS AND p.amount > 0")
    List<Payment> findSuccessPaymentsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    // ── Unified Financial Aggregates (Nguồn sự thật: Payment table) ────────────────────────

    /**
     * Tổng Gross của Owner theo b.reservationDate (ngày chơi thực tế) — thống nhất với
     * findByOwnerEmailAndFilters để danh sách và block tổng kết luôn khớp nhau.
     * Gross = tất cả payment SUCCESS có amount > 0 thuộc booking của các sân Owner sở hữu.
     * Bao gồm cả walk-in (CASH) và online (VNPay/Wallet).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.booking b JOIN b.stadium s " +
           "WHERE s.owner.ownerId = :ownerId " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount > 0 " +
           "AND b.reservationDate >= :startDate " +
           "AND b.reservationDate <= :endDate " +
           "AND b.bookingStatus IN :statuses")
    java.math.BigDecimal sumOwnerGrossByDateRangeAndStatuses(
            @Param("ownerId") Integer ownerId,
            @Param("stadiumId") Integer stadiumId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") java.util.List<com.sportvenue.entity.enums.BookingStatus> statuses);

    /**
     * Tổng Refund của Owner theo b.reservationDate (ngày chơi thực tế) — thống nhất với
     * danh sách Owner bookings. Refund = tất cả payment SUCCESS có amount < 0.
     * Trả về giá trị dương (ABS).
     */
    @Query("SELECT COALESCE(SUM(ABS(p.amount)), 0) FROM Payment p " +
           "JOIN p.booking b JOIN b.stadium s " +
           "WHERE s.owner.ownerId = :ownerId " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount < 0 " +
           "AND b.reservationDate >= :startDate " +
           "AND b.reservationDate <= :endDate " +
           "AND b.bookingStatus IN :statuses")
    java.math.BigDecimal sumOwnerRefundByDateRangeAndStatuses(
            @Param("ownerId") Integer ownerId,
            @Param("stadiumId") Integer stadiumId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") java.util.List<com.sportvenue.entity.enums.BookingStatus> statuses);

    /**
     * Tổng Gross toàn hệ thống (Admin) theo khoảng thời gian.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount > 0 " +
           "AND (:start IS NULL OR p.paidAt >= :start) " +
           "AND (:end IS NULL OR p.paidAt <= :end)")
    java.math.BigDecimal sumPlatformGrossByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Tổng Refund toàn hệ thống (Admin) theo khoảng thời gian.
     * Trả về giá trị dương (ABS).
     */
    @Query("SELECT COALESCE(SUM(ABS(p.amount)), 0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount < 0 " +
           "AND (:start IS NULL OR p.paidAt >= :start) " +
           "AND (:end IS NULL OR p.paidAt <= :end)")
    java.math.BigDecimal sumPlatformRefundByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Tổng Gross toàn hệ thống (Admin) theo b.reservationDate và toàn bộ filter.
     * Thống nhất cột ngày với danh sách — tránh lệch giữa stats và bảng booking.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.booking b " +
           "JOIN b.stadium s " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount > 0 " +
           "AND b.reservationDate >= :startDate " +
           "AND b.reservationDate <= :endDate " +
           "AND b.bookingStatus IN :bookingStatuses " +
           "AND b.paymentStatus IN :paymentStatuses " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND (:ownerId IS NULL OR s.owner.ownerId = :ownerId)")
    java.math.BigDecimal sumPlatformGrossByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("bookingStatuses") List<com.sportvenue.entity.enums.BookingStatus> bookingStatuses,
            @Param("paymentStatuses") List<com.sportvenue.entity.enums.PaymentStatus> paymentStatuses,
            @Param("stadiumId") Integer stadiumId,
            @Param("ownerId") Integer ownerId);

    /**
     * Tổng Refund toàn hệ thống (Admin) theo b.reservationDate và toàn bộ filter.
     * Trả về giá trị dương (ABS).
     */
    @Query("SELECT COALESCE(SUM(ABS(p.amount)), 0) FROM Payment p " +
           "JOIN p.booking b " +
           "JOIN b.stadium s " +
           "WHERE p.paymentStatus = com.sportvenue.entity.enums.TransactionStatus.SUCCESS " +
           "AND p.amount < 0 " +
           "AND b.reservationDate >= :startDate " +
           "AND b.reservationDate <= :endDate " +
           "AND b.bookingStatus IN :bookingStatuses " +
           "AND b.paymentStatus IN :paymentStatuses " +
           "AND (:stadiumId IS NULL OR s.stadiumId = :stadiumId) " +
           "AND (:ownerId IS NULL OR s.owner.ownerId = :ownerId)")
    java.math.BigDecimal sumPlatformRefundByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("bookingStatuses") List<com.sportvenue.entity.enums.BookingStatus> bookingStatuses,
            @Param("paymentStatuses") List<com.sportvenue.entity.enums.PaymentStatus> paymentStatuses,
            @Param("stadiumId") Integer stadiumId,
            @Param("ownerId") Integer ownerId);
}

