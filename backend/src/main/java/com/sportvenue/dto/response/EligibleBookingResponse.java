package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO trả về danh sách booking COMPLETED mà chưa được review.
 * Dùng cho UC-CUS-07: Customer kiểm tra xem có đủ điều kiện viết review không.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleBookingResponse {
    private Integer bookingId;
    private Integer stadiumId;
    private String stadiumName;
    private LocalDate reservationDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private LocalDateTime bookingDate;
}
