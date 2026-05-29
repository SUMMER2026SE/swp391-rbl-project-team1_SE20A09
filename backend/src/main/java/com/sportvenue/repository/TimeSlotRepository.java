package com.sportvenue.repository;

import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho TimeSlot entity.
 * Stub — đặc biệt quan trọng cho luồng Search và Booking.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {

    /** Lấy tất cả slot của sân theo trạng thái — dùng cho trang chi tiết sân. */
    List<TimeSlot> findByStadiumStadiumIdAndSlotStatus(Integer stadiumId, SlotStatus status);

    /**
     * Lấy slot AVAILABLE nằm TRỌN VẸN trong khoảng thời gian (startTime >= from AND endTime <= to).
     * Dùng khi Guest chọn đúng một khung giờ cụ thể và muốn xem slot nào khớp hoàn toàn.
     *
     * Ví dụ: from=08:00, to=12:00
     *   → Trả về slot 08:00–09:00, 09:00–10:00, 10:00–11:00, 11:00–12:00
     *   → KHÔNG trả về slot 07:00–09:00 (bắt đầu trước from)
     *
     * Dùng cho: UC-OV-04 (View Venue Detail) — hiển thị khung giờ trong ngày.
     */
    @Query("""
            SELECT t FROM TimeSlot t
            WHERE t.stadium.stadiumId = :stadiumId
            AND t.slotStatus = 'AVAILABLE'
            AND t.startTime >= :from
            AND t.endTime <= :to
            ORDER BY t.startTime ASC
            """)
    List<TimeSlot> findAvailableSlotsWithinRange(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Lấy slot AVAILABLE có BẤT KỲ PHẦN NÀO overlap với khoảng thời gian
     * (startTime < to AND endTime > from).
     * Dùng khi kiểm tra xung đột lịch — slot 07:00–09:00 vẫn được tính
     * nếu user tìm từ 08:00–10:00 vì có giao nhau.
     *
     * Ví dụ: from=08:00, to=10:00
     *   → Trả về slot 07:00–09:00 (overlap đầu), 08:00–09:00, 09:00–10:00, 09:30–10:30 (overlap cuối)
     *
     * Dùng cho: UC-OV-02 (Search Venue) — filter sân có slot trong khoảng giờ user chọn.
     */
    @Query("""
            SELECT t FROM TimeSlot t
            WHERE t.stadium.stadiumId = :stadiumId
            AND t.slotStatus = 'AVAILABLE'
            AND t.startTime < :to
            AND t.endTime > :from
            ORDER BY t.startTime ASC
            """)
    List<TimeSlot> findAvailableSlotsOverlapping(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Kiểm tra slot còn trống không — dùng trước khi tạo booking. */
    boolean existsBySlotIdAndSlotStatus(Integer slotId, SlotStatus status);
}
