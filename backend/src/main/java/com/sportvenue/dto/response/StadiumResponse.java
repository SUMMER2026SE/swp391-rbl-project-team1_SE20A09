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
    private String description;
    private String address;
    private BigDecimal pricePerHour;
    private Integer capacity;
    private BigDecimal averageRating;
    
    // Coordinates
    private Double latitude;
    private Double longitude;
    private Double distanceInKm; // Calculated distance if user coords provided
    
    private String sportTypeName;
    private String sportName; // From main branch
    private String firstImageUrl;
    private List<String> imageUrls; // From main branch
    
    private LocalTime openTime; // From main branch
    private LocalTime closeTime; // From main branch
    private String stadiumStatus; // From main branch
    
    private List<AmenityResponse> amenities;
}
