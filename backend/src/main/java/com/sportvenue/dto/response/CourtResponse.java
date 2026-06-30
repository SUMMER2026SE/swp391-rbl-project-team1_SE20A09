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
public class CourtResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String description;
    private BigDecimal pricePerHour;
    private Integer parentStadiumId;
    private String stadiumStatus;
    private List<String> imageUrls;
}
