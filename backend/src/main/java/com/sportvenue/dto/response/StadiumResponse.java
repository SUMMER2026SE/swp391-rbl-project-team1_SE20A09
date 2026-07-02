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
    private BigDecimal pricePerHour;
    private String stadiumStatus;
    private String approvedStatus;

    /**
     * True nếu sân đang bị chặn đặt HÔM NAY do bất kỳ cơ chế bảo trì nào (stadiumStatus,
     * complexStatus, hoặc MaintenanceSchedule có khung ngày đang active) — kể cả khi
     * {@code stadiumStatus} vẫn là "AVAILABLE" (bảo trì có khung ngày cố tình không đổi
     * stadiumStatus). Chỉ được populate ở các endpoint dành cho Owner (getMyStadiums,
     * getStadiumByIdAndOwner) — null ở các response public/search.
     */
    private Boolean underMaintenanceToday;

    // Hierarchy fields
    private String nodeType;
    private Integer complexId;
    private Integer parentStadiumId;
    
    private List<AmenityResponse> amenities;
}
