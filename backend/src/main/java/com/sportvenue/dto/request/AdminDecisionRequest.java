package com.sportvenue.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi Admin ra quyết định cuối về yêu cầu ngoại lệ.
 * Áp dụng cùng business rule approved/refundPercent như {@link OwnerReviewRequest}.
 */
@Getter
@Setter
public class AdminDecisionRequest {

    /** true = chấp nhận hoàn tiền; false = từ chối — kết thúc luồng. */
    private boolean approved;

    /**
     * Tỷ lệ hoàn tiền (50 hoặc 100).
     * Bắt buộc khi approved = true; phải null khi approved = false.
     */
    private Integer refundPercent;

    @Size(max = 2000, message = "adminNote tối đa 2000 ký tự")
    private String adminNote;
}
