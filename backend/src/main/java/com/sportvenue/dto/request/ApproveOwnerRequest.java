package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.ApprovedStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveOwnerRequest {

    @NotNull(message = "Trạng thái phê duyệt không được trống")
    private ApprovedStatus approvedStatus; // APPROVED hoặc REJECTED

    private String rejectionReason; // Bắt buộc nếu approvedStatus = REJECTED
}
