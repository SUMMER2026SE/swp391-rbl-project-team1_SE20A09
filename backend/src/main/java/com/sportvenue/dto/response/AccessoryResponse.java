package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryResponse {
    private Integer accessoryId;
    private Integer stadiumId;
    private String stadiumName;
    private String name;
    private BigDecimal pricePerUnit;
    private Integer quantity;
    private Boolean isAvailable;
}
