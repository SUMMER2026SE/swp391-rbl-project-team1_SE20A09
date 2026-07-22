package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.MatchingType;
import com.sportvenue.entity.enums.SkillLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO cho việc tạo mới một kèo ghép thể thao.
 * Định nghĩa đầy đủ validation ràng buộc dữ liệu đầu vào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMatchRequest {

    @NotNull(message = "Booking ID is required")
    private Integer bookingId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Max players is required")
    @Min(value = 2, message = "Max players must be at least 2")
    @Max(value = 50, message = "Max players cannot exceed 50")
    private Integer maxPlayers;

    @NotNull(message = "Skill level is required")
    private SkillLevel skillLevel;

    @NotNull(message = "Split price configuration is required")
    private Boolean splitPrice;

    @DecimalMin(value = "0.0", message = "Price per player must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal pricePerPlayer;

    @NotNull(message = "Matching type is required")
    @Builder.Default
    private MatchingType matchingType = MatchingType.INDIVIDUAL;

    @AssertTrue(message = "Price per player must be greater than 0 if split price is enabled")
    public boolean isPriceValidForSplit() {
        if (splitPrice == null || !splitPrice) {
            return true;
        }
        return pricePerPlayer != null && pricePerPlayer.compareTo(BigDecimal.ZERO) > 0;
    }

    @AssertTrue(message = "For Team vs Team matching, max players must be exactly 2")
    public boolean isValidMaxPlayersForTeam() {
        if (matchingType == null) {
            return true;
        }
        if (matchingType == MatchingType.TEAM_VS_TEAM) {
            return maxPlayers != null && maxPlayers == 2;
        }
        return true;
    }
}
