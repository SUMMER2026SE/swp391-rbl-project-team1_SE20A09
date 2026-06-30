package com.sportvenue.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu tạo khung giờ hàng loạt (Bulk TimeSlots).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTimeSlotRequest {

    /** Danh sách ID sân lẻ muốn áp dụng (L3). */
    private List<Integer> courtIds;

    /** Danh sách ID khu vực muốn áp dụng (L2). */
    private List<Integer> facilityIds;

    /** Nếu true, áp dụng cho toàn bộ các sân lẻ thuộc phạm vi (Facility hoặc Complex). */
    private Boolean applyToAllCourts;

    /** Danh sách khung giờ chi tiết muốn tạo. */
    @NotEmpty(message = "Danh sách khung giờ không được để trống")
    @Valid
    private List<CreateTimeSlotRequest> slots;
}
