package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về URL thanh toán VNPay đã ký — frontend redirect browser sang đó.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentUrlResponse {
    private String paymentUrl;
}
