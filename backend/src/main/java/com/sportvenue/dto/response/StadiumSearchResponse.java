package com.sportvenue.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumSearchResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String address;
    private String description;
    private BigDecimal pricePerHour;
    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal averageRating;
    private Double latitude;
    private Double longitude;
    private List<String> amenities;
    private String sportType;
    // can add ownerName or images if needed
}
