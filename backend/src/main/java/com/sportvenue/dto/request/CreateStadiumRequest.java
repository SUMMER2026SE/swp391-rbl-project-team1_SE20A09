package com.sportvenue.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStadiumRequest {

    @NotBlank(message = "Stadium name is required")
    @Size(min = 3, max = 100, message = "Stadium name must be between 3 and 100 characters")
    @Pattern(regexp = "^[^\\s].*[^\\s]$", message = "Stadium name cannot start or end with whitespace")
    private String stadiumName;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String address;

    @NotNull(message = "Sport type is required")
    @Min(value = 1, message = "Sport type is invalid")
    private Integer sportTypeId;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", message = "Price per hour must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price per hour format invalid")
    private BigDecimal pricePerHour;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Open time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime closeTime;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Digits(integer = 3, fraction = 14, message = "Latitude precision invalid")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Digits(integer = 3, fraction = 14, message = "Longitude precision invalid")
    private BigDecimal longitude;

    @NotEmpty(message = "At least one stadium image is required")
    @Size(max = 10, message = "Cannot upload more than 10 images")
    private List<@NotBlank(message = "Image URL cannot be blank")
            @Size(max = 255, message = "Image URL cannot exceed 255 characters") String> imageUrls;

    @AssertTrue(message = "Close time must be after open time")
    public boolean isCloseTimeAfterOpenTime() {
        if (openTime == null || closeTime == null) {
            return true;
        }
        return closeTime.isAfter(openTime);
    }
}
