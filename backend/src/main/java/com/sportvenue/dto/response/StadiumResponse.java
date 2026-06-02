package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String address;
    private String description;
    private Integer sportTypeId;
    private String sportName;
    private BigDecimal pricePerHour;
    private Integer capacity;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String stadiumStatus;
    private BigDecimal averageRating;
    private List<String> imageUrls;
}
