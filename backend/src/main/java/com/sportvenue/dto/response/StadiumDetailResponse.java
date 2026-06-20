package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumDetailResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String description;
    private String address;
    private BigDecimal pricePerHour;
    private BigDecimal averageRating;
    private Long totalReviews;
    
    // Coordinates
    private Double latitude;
    private Double longitude;
    
    private String sportName;
    private List<String> imageUrls;
    
    private LocalTime openTime;
    private LocalTime closeTime;
    private String stadiumStatus;
    private String approvedStatus;
    
    private List<AmenityResponse> amenities;
    private List<AccessoryResponse> accessories;
    private List<TimeSlotResponse> timeSlots;
    private OwnerInfoDto owner;
    private List<ReviewDto> recentReviews;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfoDto {
        private Integer ownerId;
        private String ownerName;
        private String phoneNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewDto {
        private Integer reviewId;
        private String userName;
        private String userAvatar;
        private Integer ratingScore;
        private String comment;
        private String ownerResponse;
        private LocalDateTime createdAt;
    }
}
