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

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
