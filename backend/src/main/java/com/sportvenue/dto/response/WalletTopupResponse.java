package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về URL thanh toán VNPay đã ký cho yêu cầu nạp tiền vào ví.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupResponse {
    private String paymentUrl;
}
