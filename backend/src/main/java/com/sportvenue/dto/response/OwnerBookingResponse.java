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
public class OwnerBookingResponse {
    private Integer id;
    private String displayId;
    private CustomerInfo customer;
    private String venue;
    private String date;
    private String time;
    private BigDecimal amount;
    private BigDecimal refundAmount;
    private BigDecimal serviceFee;
    private String paymentStatus;
    private String status;
    private String notes;
    private String playTimeRaw;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String phone;
        private String email;
    }
}
