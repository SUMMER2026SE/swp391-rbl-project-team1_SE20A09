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
    private SportTypeInfo sportType;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String stadiumStatus;
    /** True nếu Facility bị chặn đặt HÔM NAY do bảo trì (kể cả cascade từ Complex cha) — dù stadiumStatus vẫn AVAILABLE. */
    private Boolean underMaintenanceToday;
    private List<String> imageUrls;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportTypeInfo {
        private Integer sportTypeId;
        private String sportName;
    }
}
