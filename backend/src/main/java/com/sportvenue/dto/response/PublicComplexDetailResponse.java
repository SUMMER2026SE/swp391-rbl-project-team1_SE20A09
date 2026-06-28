package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicComplexDetailResponse {
    private Integer complexId;
    private String name;
    private String description;
    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;
    private String coverImageUrl;
    private String complexStatus;
    private String approvedStatus;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String ownerName;
    private String ownerPhone;
    private Double distanceInKm;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private List<SportTypeInfo> sportTypes;
    private List<AmenityInfo> amenities;
    private List<ImageInfo> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportTypeInfo {
        private Integer sportTypeId;
        private String sportName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmenityInfo {
        private Integer amenityId;
        private String name;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        private Integer imageId;
        private String imageUrl;
    }
}
