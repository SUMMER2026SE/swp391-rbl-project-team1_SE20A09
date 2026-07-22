package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEligibleBookingResponse {
    private Integer bookingId;
    private String stadiumName;
    private String complexName;
    private String address;
    private String sportName;
    private LocalDate playDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
