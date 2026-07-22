package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {
    
    private Integer bookingId;
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String subject;
    
    @NotBlank(message = "Nội dung chi tiết không được để trống")
    private String description;
}
