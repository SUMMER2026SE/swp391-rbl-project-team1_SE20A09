package com.sportvenue.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumComplexSearchRequest {

    private String keyword;
    private Integer sportTypeId;

    /** Tỉnh/thành đã chuẩn hoá (vd "Hồ Chí Minh") — exact-match, khác với keyword (free-text LIKE). */
    private String province;

    /** Quận/huyện đã chuẩn hoá (vd "Quận 1", "Cẩm Lệ") — exact-match, khác với keyword (free-text LIKE). */
    private String district;

    @DecimalMin(value = "0.0", message = "Minimum price must not be negative")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", message = "Maximum price must not be negative")
    private BigDecimal maxPrice;

    private List<Integer> amenityIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double userLat;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double userLng;

    @Positive(message = "Search radius must be a positive number")
    private Double radiusInKm;

    @Min(value = 0, message = "Page index must be >= 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 500, message = "Page size must not exceed 500")
    @Builder.Default
    private int size = 12;

    public boolean hasLocation() {
        return userLat != null && userLng != null && radiusInKm != null;
    }
}
