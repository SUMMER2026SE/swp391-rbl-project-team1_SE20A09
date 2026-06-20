package com.sportvenue.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * UC-CUS-01: Response trả về sau khi tạo chuỗi đặt sân định kỳ.
 *
 * Cơ chế all-or-nothing: nếu có ít nhất một (date, slot) bị trùng,
 * toàn bộ chuỗi sẽ KHÔNG được tạo và trả về 400. Khi thành công,
 * totalSkipped luôn bằng 0 và skippedDates là rỗng.
 *
 * Trường totalSkipped / skippedDates được giữ lại để tương thích với
 * kịch bản tương lai có thể chuyển sang best-effort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRecurringBookingResponse {

    /** UUID định danh chuỗi — chia sẻ bởi tất cả đơn thuộc cùng lần submit. */
    private String recurringGroupId;

    /** Số đơn đã tạo thành công. */
    private int totalCreated;

    /** Số (date, slot) bị bỏ qua do conflict — luôn 0 trong cơ chế hiện tại. */
    private int totalSkipped;

    /** Danh sách đơn đã tạo, sắp xếp theo ngày tăng dần. */
    private List<CreatedBookingItem> createdBookings;

    /** Danh sách (date, slot) bị conflict — rỗng trong cơ chế hiện tại. */
    private List<SkippedDateItem> skippedDates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedBookingItem {
        private Integer id;
        private LocalDate playDate;
        private LocalTime slotStart;
        private LocalTime slotEnd;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedDateItem {
        private LocalDate playDate;
        private Integer slotId;
        private LocalTime slotStart;
        private LocalTime slotEnd;
        /** Lý do bị skip — ví dụ "Khung giờ đã được đặt bởi khách hàng khác". */
        private String reason;
    }
}