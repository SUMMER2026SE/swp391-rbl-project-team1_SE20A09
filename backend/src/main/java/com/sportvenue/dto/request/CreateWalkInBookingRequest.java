package com.sportvenue.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request tạo Walk-in booking (Khách đặt tại sân).
 * reservationDate bắt buộc phải là hôm nay hoặc trong quá khứ —
 * khách vãng lai theo định nghĩa đang có mặt tại sân tại thời điểm tạo đơn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalkInBookingRequest {

    @Schema(description = "ID sân được đặt", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "stadiumId is required")
    private Integer stadiumId;

    @Schema(description = "ID khung giờ (TimeSlot)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "slotId is required")
    private Integer slotId;

    @Schema(description = "Ngày chơi — phải là hôm nay hoặc trong quá khứ (yyyy-MM-dd)", example = "2026-06-25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "reservationDate is required")
    @PastOrPresent(message = "Ngày chơi không hợp lệ — chỉ được chọn hôm nay hoặc ngày đã qua cho đơn đặt vãng lai tại sân")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @Schema(description = "Phụ kiện kèm theo (optional)")
    @Valid
    @Size(max = 50, message = "Không được chọn quá 50 phụ kiện")
    private List<AccessoryItem> accessories;
}
