package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UC-CUS-01: Một khung giờ trong weekly schedule — kèm trạng thái cho một ngày cụ thể.
 *
 * <p>Các giá trị {@code status}:</p>
 * <ul>
 *   <li>{@code AVAILABLE} — còn trống và chưa qua giờ.</li>
 *   <li>{@code BOOKED} — đã có đơn PENDING hoặc CONFIRMED trên ngày này.</li>
 *   <li>{@code PAST} — datetime bắt đầu (date + startTime) đã qua so với hiện tại.</li>
 * </ul>
 *
 * <p>KHÔNG bao gồm {@code customerName} hay thông tin định danh khách hàng —
 * chỉ phục vụ UI chọn slot.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySlotItemDto {
    private Integer slotId;
    /** HH:mm */
    private String startTime;
    /** HH:mm */
    private String endTime;
    private BigDecimal price;
    /** AVAILABLE | BOOKED | PAST */
    private String status;
}