package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {
    private Integer slotId;
    private Integer stadiumId;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal pricePerSlot;
    private String slotStatus;

    /**
     * UC-CUS-01: cờ availability cho ngày cụ thể. {@code null} khi response
     * không gắn với một ngày (ví dụ endpoint {@code /time-slots} của Owner).
     * Computed by {@code BookingService.getSlotsByDate}.
     */
    private Boolean available;
}
