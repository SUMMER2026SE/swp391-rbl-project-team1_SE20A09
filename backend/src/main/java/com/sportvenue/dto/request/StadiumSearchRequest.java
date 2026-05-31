package com.sportvenue.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumSearchRequest {
    private String keyword;
    private Integer sportTypeId;
    private String address;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Time slot filter
    private LocalDate targetDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Location/GPS filter
    private Double userLat;
    private Double userLng;
    private Double radiusInKm;

    // Amenities filter
    private List<Integer> amenityIds;

    // Pagination
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 10;
}
