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

    @DecimalMin(value = "0.0", message = "Giá thấp nhất không được âm")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", message = "Giá cao nhất không được âm")
    private BigDecimal maxPrice;

    private List<Integer> amenityIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double userLat;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double userLng;

    @Positive(message = "Bán kính tìm kiếm phải là số dương")
    private Double radiusInKm;

    @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Kích thước trang phải tối thiểu là 1")
    @Max(value = 50, message = "Kích thước trang tối đa là 50")
    @Builder.Default
    private int size = 12;

    public boolean hasLocation() {
        return userLat != null && userLng != null && radiusInKm != null;
    }
}
