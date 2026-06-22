package com.sportvenue.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UC-CUS-03: DTO cho Customer hoặc Owner yêu cầu hủy đơn đặt sân.
 * Lý do hủy là tùy chọn (nullable) nhưng không vượt quá 255 ký tự
 * để khớp với độ rộng cột {@code cancel_reason} trong bảng bookings.
 */
@Data
@NoArgsConstructor
public class CancelBookingRequest {

    @Size(max = 255, message = "Lý do hủy không được vượt quá 255 ký tự")
    private String reason;
}
