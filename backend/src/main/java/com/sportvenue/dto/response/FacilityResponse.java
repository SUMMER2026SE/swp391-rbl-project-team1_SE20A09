package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String description;
    private SportTypeResponse sportType;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String stadiumStatus;
    private List<String> imageUrls;
}
