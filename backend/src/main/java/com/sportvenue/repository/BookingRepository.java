package com.sportvenue.repository;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho Booking entity.
 * Stub — Hoàng và Lượng mở rộng thêm query khi cần.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /** Lấy lịch sử đặt sân của khách hàng — dùng cho trang "Lịch sử đặt sân". */
    @EntityGraph(attributePaths = {"stadium", "slot"})
    Page<Booking> findByUserUserIdOrderByBookingDateDesc(Integer userId, Pageable pageable);

    /** Lấy danh sách đặt sân của một sân — dùng cho Owner quản lý. */
    @EntityGraph(attributePaths = {"user", "slot"})
    Page<Booking> findByStadiumStadiumIdOrderByBookingDateDesc(Integer stadiumId, Pageable pageable);

    /** Lấy đặt sân theo trạng thái — dùng cho Owner filter Pending. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    List<Booking> findByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status);

    /** Tổng doanh thu của một sân trong khoảng thời gian — dùng cho Revenue Report. */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b
            WHERE b.stadium.stadiumId = :stadiumId
            AND b.bookingStatus = 'COMPLETED'
            AND b.bookingDate BETWEEN :from AND :to
            """)
    java.math.BigDecimal sumRevenueByStadiumAndDateRange(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Đếm số lượng đặt sân theo trạng thái — dùng cho Dashboard. */
    long countByStadiumStadiumIdAndBookingStatus(Integer stadiumId, BookingStatus status);
}
