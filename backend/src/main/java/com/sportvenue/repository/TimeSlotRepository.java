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

    /** Lấy slot còn trống trong khoảng thời gian — dùng cho Search by khung giờ. */
    @Query("""
            SELECT t FROM TimeSlot t
            WHERE t.stadium.stadiumId = :stadiumId
            AND t.slotStatus = 'AVAILABLE'
            AND t.startTime >= :from
            AND t.endTime <= :to
            ORDER BY t.startTime ASC
            """)
    List<TimeSlot> findAvailableSlots(
            @Param("stadiumId") Integer stadiumId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Kiểm tra slot còn trống không — dùng trước khi tạo booking. */
    boolean existsBySlotIdAndSlotStatus(Integer slotId, SlotStatus status);
}
