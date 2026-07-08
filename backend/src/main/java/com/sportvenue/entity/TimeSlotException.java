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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "time_slot_exceptions", indexes = {
        @Index(name = "idx_slot_exceptions_date", columnList = "exception_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotException implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Integer exceptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "start_time_override")
    private LocalTime startTimeOverride;

    @Column(name = "end_time_override")
    private LocalTime endTimeOverride;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean closed = false;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean hidden = false;
}
