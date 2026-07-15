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
public class DraftBookingResponse {
    private Integer stadiumId;
    private String stadiumName;
    private String facilityName; // Tên cơ sở (cha) - để phân biệt khi có nhiều sân trùng tên
    private String date; // dd/MM/yyyy
    private String startTime; // HH:mm
    private BigDecimal price;
}
