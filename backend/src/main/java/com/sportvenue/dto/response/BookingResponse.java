package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả về thông tin đặt sân cho Owner.
 * Bao gồm thông tin khách hàng, sân, khung giờ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Integer bookingId;

    // Thông tin khách hàng
    private Integer userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Thông tin sân
    private Integer stadiumId;
    private String stadiumName;

    // Thông tin khung giờ
    private Integer slotId;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;

    // Thông tin booking
    private BigDecimal totalPrice;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime bookingDate;
    private String note;
}
