package com.sportvenue.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * UC-CUS-01: Request tạo một đơn đặt sân đơn lẻ (single booking) cho Customer.
 *
 * Server lấy customer từ {@code UserPrincipal} — không cần customerId trong body.
 *
 * Quy tắc server-side (xem {@code BookingService.createBooking}):
 * <ul>
 *   <li>Slot phải thuộc đúng sân, slot.status phải AVAILABLE.</li>
 *   <li>Slot datetime (reservationDate + slot.startTime) phải là tương lai.</li>
 *   <li>Không có booking PENDING/CONFIRMED nào khác cho cùng
 *       (stadiumId, slotId, reservationDate).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @Schema(description = "ID sân khách muốn đặt", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "stadiumId is required")
    private Integer stadiumId;

    @Schema(description = "ID khung giờ (TimeSlot) thuộc sân trên", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "slotId is required")
    private Integer slotId;

    @Schema(description = "Ngày khách ra sân chơi (ISO yyyy-MM-dd)", example = "2026-06-25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "reservationDate is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @Schema(description = "Ghi chú tùy chọn của khách", example = "Vui lòng chuẩn bị sân trước 15 phút")
    @Size(max = 500, message = "note cannot exceed 500 characters")
    private String note;
}
