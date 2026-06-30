package com.sportvenue.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFacilityRequest {

    @NotNull(message = "Complex ID is required")
    @Min(value = 1, message = "Invalid Complex ID")
    private Integer complexId;

    @NotBlank(message = "Facility name is required")
    @Size(min = 3, max = 100, message = "Facility name must be between 3 and 100 characters")
    private String stadiumName;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Sport type is required")
    @Min(value = 1, message = "Sport type is invalid")
    private Integer sportTypeId;

    @NotNull(message = "Open time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime closeTime;

    private List<String> imageUrls;

    @AssertTrue(message = "Close time must be after open time")
    public boolean isCloseTimeAfterOpenTime() {
        if (openTime == null || closeTime == null) {
            return true;
        }
        return closeTime.isAfter(openTime);
    }
}
