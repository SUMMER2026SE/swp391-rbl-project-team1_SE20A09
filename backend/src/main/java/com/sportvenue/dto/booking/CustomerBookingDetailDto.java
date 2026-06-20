package com.sportvenue.dto.booking;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerBookingDetailDto {
    private String id;
    private String displayId;
    private String venueName;
    private String sportType;
    private String imageUrl;
    private String playDate;
    private String startTime;
    private String endTime;
    private String address;
    private BigDecimal totalPrice;
    private String status;
    private String paymentStatus;
    private String createdAt;
    private String note;
}
