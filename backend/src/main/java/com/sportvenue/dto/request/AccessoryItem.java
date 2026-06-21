package com.sportvenue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UC-CUS-01: Một phụ kiện khách chọn kèm booking.
 *
 * <p>BE chỉ nhận {@code accessoryId} + {@code quantity} — KHÔNG nhận {@code unitPrice}
 * từ client. Server lookup giá từ bảng {@code accessories} và snapshot vào
 * {@code booking_accessories.unit_price}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryItem {

    @Schema(description = "ID phụ kiện (tham chiếu accessories.accessory_id)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "accessoryId is required")
    private Integer accessoryId;

    @Schema(description = "Số lượng thuê", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
