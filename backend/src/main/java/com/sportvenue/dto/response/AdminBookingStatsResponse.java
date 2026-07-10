package com.sportvenue.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingStatsResponse {
    private long totalBookings;
    private BigDecimal totalGMV;
    private BigDecimal totalServiceFee;
}
