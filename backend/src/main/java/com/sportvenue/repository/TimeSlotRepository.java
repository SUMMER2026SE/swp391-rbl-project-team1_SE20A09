package com.sportvenue.repository;

import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
            @Param("from") LocalTime from,
            @Param("to") LocalTime to);

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
            @Param("from") LocalTime from,
            @Param("to") LocalTime to);

    /**
     * Tìm kiếm các slot trùng giờ hiện có trên sân.
     * Quy tắc nghiệp vụ (Business Rule): Không lọc theo trạng thái (status).
     * Bất kỳ slot nào đã tồn tại (kể cả đang BOOKED hay MAINTENANCE) đều không được phép tạo đè lên
     * để tránh trùng lặp dữ liệu khung giờ (duplicate slots) của cùng một sân lẻ.
     */
    @Query("""
            SELECT t FROM TimeSlot t
            WHERE t.stadium.stadiumId = :stadiumId
            AND t.startTime < :to
            AND t.endTime > :from
            ORDER BY t.startTime ASC
            """)
    List<TimeSlot> findOverlappingSlots(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalTime from,
            @Param("to") LocalTime to);

    /** Kiểm tra slot còn trống không — dùng trước khi tạo booking. */
    boolean existsBySlotIdAndSlotStatus(Integer slotId, SlotStatus status);

    /**
     * UC-CUS-01: Lấy slot kèm PESSIMISTIC_WRITE để tránh race condition khi tạo booking.
     * Dùng trong BookingServiceImpl.createBooking — khoá row slot để 2 request đồng thời
     * serialize qua bước conflict-check + insert.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.slotId = :slotId")
    Optional<TimeSlot> findByIdForUpdate(@Param("slotId") Integer slotId);

    @Query("SELECT s FROM TimeSlot s JOIN FETCH s.stadium st JOIN FETCH st.owner o JOIN FETCH o.user u WHERE s.slotId = :slotId")
    Optional<TimeSlot> findByIdWithOwner(@Param("slotId") Integer slotId);
}
