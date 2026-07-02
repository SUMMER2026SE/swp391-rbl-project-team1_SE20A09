package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String reason;
    /** {@code true} nếu endDate == null. */
    private Boolean indefinite;
    /** {@code true} nếu hôm nay nằm trong khoảng [startDate, endDate ?? +inf]. */
    private Boolean active;
    private LocalDateTime createdAt;
}
