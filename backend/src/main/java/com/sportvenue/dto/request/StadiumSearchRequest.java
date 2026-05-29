package com.sportvenue.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumSearchRequest {
    private String keyword;
    private String address;
    private String sportType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private List<String> amenities;
}
