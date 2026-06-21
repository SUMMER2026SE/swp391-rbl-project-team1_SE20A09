package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * UC-CUS-01: Một ngày trong weekly slots response.
 *
 * <p>Chứa thông tin ngày, tên thứ (tiếng Việt) và danh sách khung giờ
 * kèm trạng thái {@code AVAILABLE | BOOKED | PAST}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySlotDayDto {
    /** ISO yyyy-MM-dd. */
    private String date;

    /** Tên thứ tiếng Việt — ví dụ "Thứ 2", "Chủ nhật". */
    private String dayName;

    /** Toàn bộ khung giờ của sân cho ngày này (sắp xếp theo startTime). */
    private List<WeeklySlotItemDto> slots;
}