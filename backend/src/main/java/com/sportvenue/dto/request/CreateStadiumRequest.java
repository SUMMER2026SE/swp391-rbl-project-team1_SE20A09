package com.sportvenue.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStadiumRequest {

    @NotBlank(message = "Stadium name is required")
    @Size(max = 100, message = "Stadium name cannot exceed 100 characters")
    private String stadiumName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Sport type is required")
    private Integer sportTypeId;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerHour;

    private String description;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private LocalTime openTime;

    private LocalTime closeTime;
}
