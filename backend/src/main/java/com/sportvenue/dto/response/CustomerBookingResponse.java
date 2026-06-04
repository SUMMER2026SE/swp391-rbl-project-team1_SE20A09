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
public class CustomerBookingResponse {
    private Integer id;
    private String bookingCode;
    private String venueName;
    private String venueImage;
    private String sportType;
    private String location;
    private String date;
    private String startTime;
    private String endTime;
    private String time;
    private BigDecimal totalPrice;
    private BigDecimal pricePerHour;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String note;
    private String bookingDate;
    private Boolean hasReviewed;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
