package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueRevenueDto {
    private Integer stadiumId;
    private String stadiumName;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private Double occupancy;
    private String trend;
}
