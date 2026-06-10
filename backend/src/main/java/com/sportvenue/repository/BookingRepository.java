package com.sportvenue.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;

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
    @EntityGraph(attributePaths = {"stadium", "stadium.sportType", "stadium.images", "slot"})
    Page<Booking> findByUserUserIdOrderByBookingDateDesc(Integer userId, Pageable pageable);

    /** Lịch sắp tới — slot chưa kết thúc, đơn Pending hoặc Confirmed. */
    @EntityGraph(attributePaths = {"stadium", "stadium.sportType", "stadium.images", "slot"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.user.userId = :userId
            AND b.bookingStatus IN (com.sportvenue.entity.enums.BookingStatus.PENDING,
                                    com.sportvenue.entity.enums.BookingStatus.CONFIRMED)
            AND b.slot.endTime >= :now
            ORDER BY b.slot.startTime ASC
            """)
    List<Booking> findUpcomingByUserId(
            @Param("userId") Integer userId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    long countByUserUserId(Integer userId);

    @Query("""
            SELECT COUNT(DISTINCT b.stadium.stadiumId) FROM Booking b
            WHERE b.user.userId = :userId
            AND b.bookingStatus = com.sportvenue.entity.enums.BookingStatus.COMPLETED
            """)
    long countDistinctCompletedVenues(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"stadium", "stadium.sportType", "slot"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.user.userId = :userId
            AND b.bookingStatus = com.sportvenue.entity.enums.BookingStatus.COMPLETED
            """)
    List<Booking> findCompletedByUserId(@Param("userId") Integer userId);

    /** Tổng số phút chơi từ các booking hoàn thành — dùng cho PersonalStats. */
    @Query(value = """
            SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (s.end_time - s.start_time)) / 60), 0)
            FROM bookings b
            JOIN time_slots s ON b.slot_id = s.slot_id
            WHERE b.user_id = :userId
            AND b.booking_status = 'COMPLETED'
            """, nativeQuery = true)
    long sumCompletedPlayMinutes(@Param("userId") Integer userId);

    /** Môn thể thao chơi nhiều nhất — dùng cho PersonalStats. Returns [sportName, count]. */
    @Query("""
            SELECT b.stadium.sportType.sportName, COUNT(b)
            FROM Booking b
            WHERE b.user.userId = :userId
            AND b.bookingStatus = com.sportvenue.entity.enums.BookingStatus.COMPLETED
            GROUP BY b.stadium.sportType.sportName
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findTopSportByUserId(@Param("userId") Integer userId, Pageable pageable);

    /** Lấy danh sách đặt sân của một sân — dùng cho Owner quản lý. */
    @EntityGraph(attributePaths = {"user", "slot"})
    Page<Booking> findByStadiumStadiumIdOrderByBookingDateDesc(Integer stadiumId, Pageable pageable);

    /** Lấy đặt sân theo trạng thái — dùng cho Owner filter Pending. */
    @EntityGraph(attributePaths = {"user", "stadium", "stadium.sportType", "slot"})
    Page<Booking> findByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status, Pageable pageable);

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
            AND b.bookingStatus = com.sportvenue.entity.enums.BookingStatus.COMPLETED
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
            AND b.bookingStatus = com.sportvenue.entity.enums.BookingStatus.COMPLETED
            AND b.slot.startTime BETWEEN :from AND :to
            """)
    BigDecimal sumRevenueBySlotDate(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Đếm số lượng đặt sân theo trạng thái — dùng cho Dashboard. */
    long countByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status);

    /**
     * Lấy booking của nhiều sân cùng lúc — dùng cho Owner xem tất cả booking.
     * Hỗ trợ filter theo status (optional, null = tất cả).
     */
    @EntityGraph(attributePaths = {"user", "stadium", "stadium.sportType", "slot"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.stadium.stadiumId IN :stadiumIds
            AND (:status IS NULL OR b.bookingStatus = :status)
            ORDER BY b.bookingDate DESC
            """)
    Page<Booking> findByStadiumStadiumIdInOrderByBookingDateDesc(
            @Param("stadiumIds") List<Integer> stadiumIds,
            @Param("status") BookingStatus status,
            Pageable pageable);

    /** Lấy danh sách đặt sân thuộc các sân mà Owner sở hữu. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    List<Booking> findByStadiumOwnerUserEmailOrderByBookingDateDesc(String email);

    /** Lấy danh sách đặt sân của tất cả các sân thuộc Owner có phân trang và filter status */
    @EntityGraph(attributePaths = {"user", "stadium", "stadium.sportType", "slot"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.stadium.owner.ownerId = :ownerId
            AND (:status IS NULL OR b.bookingStatus = :status)
            ORDER BY b.bookingDate DESC
            """)
    Page<Booking> findByOwnerIdAndStatus(
            @Param("ownerId") Integer ownerId,
            @Param("status") BookingStatus status,
            Pageable pageable);
}
