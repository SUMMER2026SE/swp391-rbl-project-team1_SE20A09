package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexResponse {
    private Integer complexId;
    private Integer ownerId;
    private String name;
    private String description;
    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;
    private String coverImageUrl;
    private String complexStatus;
    private String approvedStatus;
    private String rejectionReason;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Set<Integer> sportTypeIds;
    private List<String> sportNames;
    private Set<Integer> amenityIds;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
