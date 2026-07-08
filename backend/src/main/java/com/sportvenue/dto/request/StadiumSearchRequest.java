package com.sportvenue.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import com.sportvenue.entity.enums.StadiumStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumSearchRequest {
    private String keyword;
    private Integer sportTypeId;
    private String address;

    /** Tỉnh/thành đã chuẩn hoá (vd "Hồ Chí Minh") — exact-match, khác với {@link #address} (free-text LIKE). */
    private String province;

    /** Quận/huyện đã chuẩn hoá (vd "Quận 1", "Cẩm Lệ") — exact-match, khác với {@link #address} (free-text LIKE). */
    private String district;
    @Min(value = 0, message = "Giá thấp nhất không được âm")
    private BigDecimal minPrice;
    
    @Min(value = 0, message = "Giá cao nhất không được âm")
    private BigDecimal maxPrice;
    private StadiumStatus status;

    // Time slot filter
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // Location/GPS filter
    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double userLat;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double userLng;

    @Positive(message = "Bán kính tìm kiếm phải là số dương")
    private Double radiusInKm;

    // Amenities filter
    private List<Integer> amenityIds;

    // Sort (optional) — chỉ chấp nhận field trong whitelist ở PublicStadiumServiceImpl,
    // giá trị lạ bị bỏ qua (không ném lỗi) để giữ tương thích ngược.
    private String sortBy;

    /** "ASC" (mặc định) hoặc "DESC". */
    private String sortDirection;

    // Pagination
    @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private int page = 0;
    
    @Min(value = 1, message = "Kích thước trang phải lớn hơn hoặc bằng 1")
    @Max(value = 500, message = "Kích thước trang không được vượt quá 500")
    @Builder.Default
    private int size = 10;

    @AssertTrue(message = "Giá cao nhất phải lớn hơn hoặc bằng giá thấp nhất")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) {
            return true;
        }
        return maxPrice.compareTo(minPrice) >= 0;
    }

    @AssertTrue(message = "Phải cung cấp cả vĩ độ và kinh độ, hoặc không cung cấp gì cả")
    public boolean isLocationValid() {
        int count = 0;
        if (userLat != null) {
            count++;
        }
        if (userLng != null) {
            count++;
        }
        return count == 0 || count == 2;
    }
}
