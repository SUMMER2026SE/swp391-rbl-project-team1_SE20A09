package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReportRequest {

    @NotNull(message = "Trạng thái xử lý không được để trống")
    private ReportStatus status;

    @Size(max = 2000, message = "Ghi chú xử lý không được vượt quá 2000 ký tự")
    private String resolutionNote;
}
