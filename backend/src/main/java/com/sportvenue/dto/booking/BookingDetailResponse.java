package com.sportvenue.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * UC-CUS-01: Response trả về sau khi tạo booking đơn lẻ — chứa đầy đủ
 * thông tin FE cần để hiển thị ngay trong tab "Lịch sử đặt sân" (không cần
 * gọi thêm API chi tiết).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    @Schema(description = "ID đơn đặt sân vừa tạo", example = "42")
    private Integer bookingId;

    @Schema(description = "Mã hiển thị (BK + 6 chữ số)", example = "BK000042")
    private String displayId;

    @Schema(description = "Ngày khách ra sân chơi")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @Schema(description = "Thông tin khung giờ đã đặt")
    private SlotInfo slot;

    @Schema(description = "Thông tin sân đã đặt")
    private StadiumInfo stadium;

    @Schema(description = "Tổng tiền", example = "500000.00")
    private BigDecimal totalPrice;

    @Schema(description = "Trạng thái đơn — sau createBooking luôn là PENDING", example = "pending")
    private String status;

    @Schema(description = "Trạng thái thanh toán — sau createBooking luôn là UNPAID", example = "unpaid")
    private String paymentStatus;

    @Schema(description = "Ghi chú của khách (nếu có)")
    private String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotInfo {
        private Integer slotId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StadiumInfo {
        private Integer stadiumId;
        private String stadiumName;
        private String address;
    }
}
