package com.sportvenue.entity;

import com.sportvenue.entity.enums.SlotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng time_slots.
 * Đại diện cho một khung giờ cụ thể của sân.
 * Đây là đơn vị nhỏ nhất để đặt sân — mỗi booking gắn với một slot.
 */
@Entity
@Table(name = "time_slots", indexes = {
        @Index(name = "idx_time_slots_stadium", columnList = "stadium_id"),
        @Index(name = "idx_time_slots_status", columnList = "slot_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Integer slotId;

    /** Sân chứa khung giờ này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    /** Thời điểm bắt đầu của khung giờ. */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** Thời điểm kết thúc của khung giờ. */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /** Trạng thái hiện tại của slot — dùng để kiểm tra trước khi đặt. */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot_status", nullable = false, length = 20)
    @Builder.Default
    private SlotStatus slotStatus = SlotStatus.AVAILABLE;
}
