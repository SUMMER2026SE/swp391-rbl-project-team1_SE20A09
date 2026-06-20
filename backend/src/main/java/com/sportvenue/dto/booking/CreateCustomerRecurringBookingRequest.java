package com.sportvenue.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * UC-CUS-01: Request cho chức năng đặt sân định kỳ.
 * Khách hàng chọn sân, khoảng ngày, các thứ trong tuần, một hoặc nhiều khung giờ,
 * hệ thống sẽ tạo N đơn đặt sân liên kết bởi recurringGroupId (UUID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRecurringBookingRequest {

    /** Sân được đặt — bắt buộc. */
    @NotNull(message = "Vui lòng chọn sân")
    private Integer stadiumId;

    /** Ngày bắt đầu chuỗi đặt sân — bắt buộc, không được trước hôm nay. */
    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** Ngày kết thúc chuỗi đặt sân — bắt buộc, không quá 90 ngày sau startDate. */
    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Danh sách các thứ trong tuần cần lặp — ví dụ [MONDAY, WEDNESDAY, FRIDAY].
     * Bắt buộc, tối đa 7 phần tử.
     */
    @NotEmpty(message = "Vui lòng chọn ít nhất một thứ trong tuần")
    @Size(max = 7, message = "Không được chọn quá 7 thứ")
    private List<DayOfWeek> daysOfWeek;

    /**
     * Danh sách slotId cần đặt cho MỖI ngày trong chuỗi — bắt buộc, ít nhất 1 slot.
     */
    @NotEmpty(message = "Vui lòng chọn ít nhất một khung giờ")
    private List<Integer> slotIds;

    /** Ghi chú (tùy chọn, tối đa 500 ký tự) — áp dụng cho TẤT CẢ đơn trong chuỗi. */
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    /**
     * Validation tổng hợp:
     * - startDate không được trước hôm nay
     * - endDate không được trước startDate
     * - (endDate - startDate) tối đa 90 ngày
     * Trả về false sẽ khiến Bean Validation ném MethodArgumentNotValidException → 400.
     */
    @AssertTrue(message = "Khoảng ngày không hợp lệ: startDate phải >= hôm nay, endDate >= startDate, tối đa 90 ngày")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            // @NotNull đã xử lý — bỏ qua để tránh message trùng lặp.
            return true;
        }
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            return false;
        }
        if (endDate.isBefore(startDate)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return days <= 90;
    }
}