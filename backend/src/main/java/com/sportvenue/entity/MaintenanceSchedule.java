package com.sportvenue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "maintenance_schedules", indexes = {
        @Index(name = "idx_maintenance_schedules_stadium", columnList = "stadium_id"),
        @Index(name = "idx_maintenance_schedules_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Integer maintenanceId;

    /** Sân (Facility/Court) bị bảo trì — null nếu khung này gắn ở cấp Complex ({@link #complex}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id")
    private Stadium stadium;

    /** Tổ hợp bị bảo trì (cascade toàn bộ Facility + Court con) — null nếu gắn ở cấp Stadium ({@link #stadium}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id")
    private StadiumComplex complex;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** NULL = vô thời hạn. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** NULL = tính từ đầu ngày {@link #startDate} (00:00). */
    @Column(name = "start_time")
    private LocalTime startTime;

    /** NULL = tính đến hết ngày {@link #endDate} (23:59:59), hoặc vô thời hạn nếu endDate cũng NULL. */
    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(LocalDate.of(9999, 12, 31), LocalTime.MAX);

    /** Thời điểm bắt đầu hiệu lực — kết hợp startDate + startTime (mặc định 00:00 nếu không set giờ). */
    public LocalDateTime effectiveStart() {
        return LocalDateTime.of(startDate, startTime != null ? startTime : LocalTime.MIN);
    }

    /** Thời điểm kết thúc hiệu lực — kết hợp endDate + endTime (mặc định 23:59:59 nếu không set giờ), hoặc vô thời hạn. */
    public LocalDateTime effectiveEnd() {
        if (endDate == null) {
            return FAR_FUTURE;
        }
        return LocalDateTime.of(endDate, endTime != null ? endTime : LocalTime.MAX);
    }

    /** True nếu khoảng [rangeStart, rangeEndExclusive) chồng lấn với khoảng hiệu lực của lịch bảo trì này. */
    public boolean overlaps(LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        return effectiveStart().isBefore(rangeEndExclusive) && rangeStart.isBefore(effectiveEnd());
    }

    /** True nếu {@code instant} nằm trong khoảng hiệu lực — dùng để check "đang bảo trì NGAY BÂY GIỜ". */
    public boolean covers(LocalDateTime instant) {
        return !effectiveStart().isAfter(instant) && instant.isBefore(effectiveEnd());
    }
}
