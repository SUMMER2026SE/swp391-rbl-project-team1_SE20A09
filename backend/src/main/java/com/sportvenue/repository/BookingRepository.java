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

import java.math.BigDecimal;
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

    /** Lấy tất cả booking của tất cả sân thuộc Owner — dùng cho UC-OWN-06 overview. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    Page<Booking> findByStadiumOwnerOwnerIdOrderByBookingDateDesc(Integer ownerId, Pageable pageable);

    /** Lấy booking theo owner + filter status — dùng cho UC-OWN-06 filter. */
    @EntityGraph(attributePaths = {"user", "stadium", "slot"})
    Page<Booking> findByStadiumOwnerOwnerIdAndBookingStatusOrderByBookingDateDesc(
            Integer ownerId, BookingStatus status, Pageable pageable);

    /** Lấy tất cả booking của một sân (không filter status) — dùng cho UC-OWN-06. */
    @EntityGraph(attributePaths = {"user", "slot"})
    Page<Booking> findByStadiumStadiumIdOrderByBookingDateDesc(Integer stadiumId, Pageable pageable);

    /** Lấy booking theo sân + filter status — dùng cho UC-OWN-06. */
    @EntityGraph(attributePaths = {"user", "slot"})
    Page<Booking> findByStadiumStadiumIdAndBookingStatusOrderByBookingDateDesc(
            Integer stadiumId, BookingStatus status, Pageable pageable);
}
