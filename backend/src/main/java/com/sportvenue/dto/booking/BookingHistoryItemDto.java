package com.sportvenue.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UC-CUS-01: DTO tóm tắt một đơn đặt sân — dùng cho trang "Lịch sử đặt sân" của Customer.
 * Shape khớp với {@code BookingHistoryItem} ở Frontend
 * ({@code frontend/src/lib/bookings-api.ts}).
 *
 * <p>Các field phải khớp với frontend để không phải mapping lại:</p>
 * <ul>
 *   <li>{@code id} — String (bookingId) để khớp với key React.</li>
 *   <li>{@code displayId} — mã hiển thị {@code BK000042}.</li>
 *   <li>{@code price} — {@link BigDecimal} (Jackson serialize thành number).</li>
 *   <li>{@code status} — lowercase string ("pending" / "confirmed" / "completed" / "cancelled").</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryItemDto {

    @Schema(description = "ID đơn đặt sân (String)", example = "42")
    private String id;

    @Schema(description = "Mã hiển thị (BK + 6 chữ số)", example = "BK000042")
    private String displayId;

    @Schema(description = "Tên sân đã đặt", example = "Sân bóng đá Mini XYZ")
    private String venue;

    @Schema(description = "Tên môn thể thao", example = "Bóng đá")
    private String sportType;

    @Schema(description = "URL ảnh đại diện của sân (ảnh đầu tiên hoặc placeholder)")
    private String imageUrl;

    @Schema(description = "Ngày khách ra sân (yyyy-MM-dd)", example = "2026-06-25")
    private String date;

    @Schema(description = "Khung giờ chơi (HH:mm - HH:mm)", example = "18:00 - 20:00")
    private String time;

    @Schema(description = "Địa chỉ sân", example = "123 Nguyễn Văn A, Q.1, TP.HCM")
    private String location;

    @Schema(description = "Tổng tiền thanh toán", example = "500000.00")
    private BigDecimal price;

    @Schema(description = "Trạng thái đơn (lowercase)", example = "pending",
            allowableValues = {"pending", "confirmed", "completed", "cancelled"})
    private String status;
}