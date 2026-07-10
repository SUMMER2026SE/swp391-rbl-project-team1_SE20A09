package com.sportvenue.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingResponse {
    private Integer bookingId;
    private String customerName;
    private String customerEmail;
    private String stadiumName;
    private String ownerName;
    private BigDecimal totalPrice;
    private BigDecimal serviceFee;
    private String bookingStatus;
    private String paymentStatus;
    private LocalDateTime bookingDate;
    private LocalDate reservationDate;
    private String timeSlot;
    private String note;
    private String cancelReason;
}
