package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * UC-CUS-01: Lịch khung giờ theo tuần của một sân — phục vụ
 * {@code GET /api/v1/stadiums/{id}/weekly-slots?weekStart=YYYY-MM-DD}.
 *
 * <p>Trả về 7 ngày (thứ 2 → chủ nhật) của tuần chứa {@code weekStart}.
 * Mỗi ngày liệt kê toàn bộ khung giờ của sân kèm trạng thái
 * {@code AVAILABLE | BOOKED | PAST} — KHÔNG trả về {@code customerName}
 * vì lý do bảo mật.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySlotResponse {
    /** Ngày đầu tuần (thứ 2) — ISO yyyy-MM-dd. */
    private String weekStart;

    /** Ngày cuối tuần (chủ nhật) — ISO yyyy-MM-dd. */
    private String weekEnd;

    /** Danh sách 7 ngày trong tuần, theo thứ tự thứ 2 → chủ nhật. */
    private List<WeeklySlotDayDto> days;
}