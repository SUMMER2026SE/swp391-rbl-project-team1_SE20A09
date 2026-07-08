package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotExceptionResponse {
    private Integer exceptionId;
    private Integer slotId;
    private LocalDate exceptionDate;
    private BigDecimal priceOverride;
    private LocalTime startTimeOverride;
    private LocalTime endTimeOverride;
    private Boolean closed;
    private Boolean hidden;
}
