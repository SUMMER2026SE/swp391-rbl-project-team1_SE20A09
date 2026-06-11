package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {
    
    @NotNull(message = "ID đơn đặt sân không được để trống")
    private Integer bookingId;
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String subject;
    
    @NotBlank(message = "Nội dung chi tiết không được để trống")
    private String description;
}
