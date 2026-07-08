package com.sportvenue.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    @Size(max = 255, message = "Lý do hủy không được vượt quá 255 ký tự")
    private String reason;

    private com.sportvenue.entity.enums.RefundReasonType reasonType;

    @Size(max = 500, message = "Đường dẫn bằng chứng không được vượt quá 500 ký tự")
    private String proofUrl;
}
