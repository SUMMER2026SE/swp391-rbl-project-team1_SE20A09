package com.sportvenue.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi Owner duyệt (chấp nhận/từ chối) yêu cầu ngoại lệ.
 *
 * <p>Business rule được validate ở Service:
 * <ul>
 *   <li>{@code approved = false} → {@code refundPercent} phải {@code null}</li>
 *   <li>{@code approved = true}  → {@code refundPercent} phải là {@code 50} hoặc {@code 100}</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
public class OwnerReviewRequest {

    /** true = chấp nhận hoàn tiền; false = từ chối. */
    private boolean approved;

    /**
     * Tỷ lệ hoàn tiền (50 hoặc 100).
     * Bắt buộc khi approved = true; phải null khi approved = false.
     */
    private Integer refundPercent;

    @Size(max = 1000, message = "ownerNote tối đa 1000 ký tự")
    private String ownerNote;
}
