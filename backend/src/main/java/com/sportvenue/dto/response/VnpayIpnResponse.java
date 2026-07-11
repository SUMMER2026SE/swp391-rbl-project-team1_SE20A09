package com.sportvenue.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về cho VNPay IPN — VNPay yêu cầu đúng field name PascalCase
 * ({@code RspCode}/{@code Message}), khác quy ước camelCase còn lại của dự án.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;
}
