package com.sportvenue.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sportvenue.entity.enums.SkillLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO cho việc tạo mới một kèo ghép thể thao.
 * Định nghĩa đầy đủ validation ràng buộc dữ liệu đầu vào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMatchRequest {

    @NotNull(message = "Stadium ID is required")
    @Min(value = 1, message = "Invalid Stadium ID")
    private Integer stadiumId;

    @NotNull(message = "Sport Type ID is required")
    @Min(value = 1, message = "Invalid Sport Type ID")
    private Integer sportTypeId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Play date is required")
    @FutureOrPresent(message = "Play date must be in the present or future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate playDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotNull(message = "Max players is required")
    @Min(value = 2, message = "Max players must be at least 2")
    private Integer maxPlayers;

    @NotNull(message = "Skill level is required")
    private SkillLevel skillLevel;

    @NotNull(message = "Split price configuration is required")
    private Boolean splitPrice;

    @DecimalMin(value = "0.0", message = "Price per player must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal pricePerPlayer;

    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    @AssertTrue(message = "Price per player must be greater than 0 if split price is enabled")
    public boolean isPriceValidForSplit() {
        if (splitPrice == null || !splitPrice) {
            return true;
        }
        return pricePerPlayer != null && pricePerPlayer.compareTo(BigDecimal.ZERO) > 0;
    }
}
