package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportTypeResponse {
    private Integer sportTypeId;
    private String sportName;
    private String nameEn;
    private String sportCode;
    private String description;
    private Boolean isActive;
    private Boolean isFootballType;
    private LocalDateTime createdAt;
}
