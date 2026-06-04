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
}
