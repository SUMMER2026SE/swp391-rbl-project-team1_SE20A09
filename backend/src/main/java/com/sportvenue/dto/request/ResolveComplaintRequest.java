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
public class ResolveComplaintRequest {
    
    @NotBlank(message = "Nội dung giải quyết không được để trống")
    private String resolution;
}
