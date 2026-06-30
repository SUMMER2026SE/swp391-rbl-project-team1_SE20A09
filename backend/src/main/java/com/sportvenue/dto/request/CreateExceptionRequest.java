package com.sportvenue.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExceptionRequest {
    private BigDecimal priceOverride;
    private LocalTime startTimeOverride;
    private LocalTime endTimeOverride;
    private Boolean closed;
    private Boolean hidden;
}
