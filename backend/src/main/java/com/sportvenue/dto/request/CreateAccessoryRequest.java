package com.sportvenue.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccessoryRequest {

    @NotBlank(message = "Tên phụ kiện không được để trống")
    private String name;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.0", message = "Giá thuê không được nhỏ hơn 0")
    private BigDecimal pricePerUnit;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    private Integer quantity;

    @Builder.Default
    private Boolean isAvailable = true;
}
