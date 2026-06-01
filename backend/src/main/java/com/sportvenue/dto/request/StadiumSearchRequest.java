package com.sportvenue.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

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
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // Location/GPS filter
    private Double userLat;
    private Double userLng;
    private Double radiusInKm;

    // Amenities filter
    private List<Integer> amenityIds;

    // Pagination
    @Min(0)
    @Builder.Default
    private int page = 0;
    
    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 10;
}
