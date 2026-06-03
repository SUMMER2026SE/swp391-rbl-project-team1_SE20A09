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
    private Integer sportTypeId;
    private String address;
    private BigDecimal pricePerHour;
    private Integer capacity;
    private BigDecimal averageRating;
    
    // Coordinates
    private Double latitude;
    private Double longitude;
    private Double distanceInKm; // Calculated distance if user coords provided
    
    private String sportName; 
    private String firstImageUrl;
    private List<String> imageUrls; 
    
    private LocalTime openTime; 
    private LocalTime closeTime; 
    private String stadiumStatus; 
    
    private List<AmenityResponse> amenities;
}
