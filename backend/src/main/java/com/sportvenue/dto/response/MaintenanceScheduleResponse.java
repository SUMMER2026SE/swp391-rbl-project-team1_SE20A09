package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceScheduleResponse {
    private Integer maintenanceId;
    /** Null nếu khung này gắn ở cấp Complex — xem {@link #complexId}. */
    private Integer stadiumId;
    /** Null nếu khung này gắn ở cấp Stadium — xem {@link #stadiumId}. */
    private Integer complexId;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Null = tính từ đầu ngày startDate. */
    private LocalTime startTime;
    /** Null = tính đến hết ngày endDate. */
    private LocalTime endTime;
    private String reason;
    /** {@code true} nếu endDate == null. */
    private Boolean indefinite;
    /** {@code true} nếu thời điểm hiện tại nằm trong khoảng hiệu lực [effectiveStart, effectiveEnd ?? +inf]. */
    private Boolean active;
    private LocalDateTime createdAt;
}
