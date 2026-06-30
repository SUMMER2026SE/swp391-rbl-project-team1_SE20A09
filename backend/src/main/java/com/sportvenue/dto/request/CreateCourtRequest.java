package com.sportvenue.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourtRequest {

    @NotNull(message = "Parent Facility ID is required")
    @Min(value = 1, message = "Invalid Facility ID")
    private Integer parentStadiumId;

    @NotBlank(message = "Court name is required")
    @Size(min = 1, max = 100, message = "Court name must be between 1 and 100 characters")
    private String stadiumName;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", message = "Price per hour must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price per hour format invalid")
    private BigDecimal pricePerHour;

    private List<String> imageUrls;
}
