package com.sportvenue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UC-CUS-06: DTO trả về cho Customer khi xem trước số tiền hoàn trước khi
 * xác nhận huỷ đơn đặt sân.
 *
 * <p>Shape đơn giản — chỉ chứa các trường FE cần hiển thị trong dialog "Xem trước
 * hoàn tiền" mà KHÔNG cần thông tin stadium/customer như {@code RefundResponse}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả xem trước hoàn tiền cho Customer trước khi huỷ đơn")
public class RefundPreviewResponse {

    @Schema(description = "ID booking", example = "42")
    private Integer bookingId;

    @Schema(description = "Tổng tiền đã thanh toán", example = "500000.00")
    private BigDecimal originalAmount;

    @Schema(description = "Số tiền hoàn dự kiến", example = "250000.00")
    private BigDecimal refundAmount;

    @Schema(description = "Phần trăm hoàn", example = "50", allowableValues = {"0", "50", "100"})
    private int refundPercent;

    @Schema(description = "Thời điểm chơi (reservationDate + slot.startTime)",
            example = "2026-06-25T18:00:00")
    private LocalDateTime playTime;

    @Schema(description = "Giời gian hiện tại server (ISO)", example = "2026-06-22T03:00:00")
    private LocalDateTime previewedAt;

    @Schema(description = "Giải thích bằng tiếng Việt cho phần trăm hoàn",
            example = "Huỷ trước 24h: hoàn 100%")
    private String reason;
}