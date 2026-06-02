package com.sportvenue.repository;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho Booking entity.
 * Stub — Hoàng và Lượng mở rộng thêm query khi cần.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /** Tìm kiếm đơn đặt sân kèm theo Pessimistic Write Lock để tránh Race Condition (Double Refund) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Integer id);

    /** Lấy lịch sử đặt sân của khách hàng — dùng cho trang "Lịch sử đặt sân". */
    @EntityGraph(attributePaths = {"stadium", "slot"})
    Page<Booking> findByUserUserIdOrderByBookingDateDesc(Integer userId, Pageable pageable);

    /** Lấy danh sách đặt sân của một sân — dùng cho Owner quản lý. */
    @EntityGraph(attributePaths = {"user", "slot"})
    Page<Booking> findByStadiumStadiumIdOrderByBookingDateDesc(Integer stadiumId, Pageable pageable);

    /** Lấy đặt sân theo trạng thái — dùng cho Owner filter Pending. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    List<Booking> findByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status);

    /**
     * Tổng doanh thu theo NGÀY ĐẶT (bookingDate) — thời điểm khách tạo đơn.
     *
     * Phù hợp khi báo cáo theo "doanh thu ghi nhận trong ngày" (kế toán dồn tích).
     * Không phản ánh ngày khách thực sự ra sân chơi.
     *
     * Dùng cho: UC-OWN-10 (Revenue Report) nếu product owner muốn thống kê theo ngày đặt.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b
            WHERE b.stadium.stadiumId = :stadiumId
            AND b.bookingStatus = 'COMPLETED'
            AND b.bookingDate BETWEEN :from AND :to
            """)
    BigDecimal sumRevenueByBookingDate(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Tổng doanh thu theo NGÀY CHƠI (slot.startTime) — thời điểm khách thực sự ra sân.
     *
     * Phù hợp khi báo cáo theo "doanh thu phát sinh trong ngày" (thực tế vận hành sân).
     * Ví dụ: khách đặt ngày 1 nhưng chơi ngày 5 → tính vào doanh thu ngày 5.
     *
     * Dùng cho: UC-OWN-10 (Revenue Report) nếu product owner muốn thống kê theo ngày chơi.
     * Đây thường là cách tính phù hợp hơn cho báo cáo vận hành sân thể thao.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b
            WHERE b.stadium.stadiumId = :stadiumId
            AND b.bookingStatus = 'COMPLETED'
            AND b.slot.startTime BETWEEN :from AND :to
            """)
    BigDecimal sumRevenueBySlotDate(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Đếm số lượng đặt sân theo trạng thái — dùng cho Dashboard. */
    long countByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status);

    /** Lấy danh sách đặt sân thuộc các sân mà Owner sở hữu. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    List<Booking> findByStadiumOwnerUserEmailOrderByBookingDateDesc(String email);

    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.stadium.owner.user.email = :ownerEmail " +
           "AND (:stadiumId IS NULL OR b.stadium.stadiumId = :stadiumId) " +
           "AND (b.bookingStatus = 'COMPLETED' OR b.bookingStatus = 'CONFIRMED') " +
           "AND b.paymentStatus = 'PAID' " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate")
    Long countBookingsForRevenue(@Param("ownerEmail") String ownerEmail,
                                 @Param("stadiumId") Integer stadiumId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}
