package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Người bị báo cáo không được để trống")
    private Integer reporteeId;

    private Integer bookingId;
    private Integer matchRequestId;
    private Integer joinRequestId;
    private Integer stadiumId;

    @NotNull(message = "Loại báo cáo không được để trống")
    private ReportCategory category;

    @NotBlank(message = "Nội dung báo cáo không được để trống")
    @Size(max = 2000, message = "Nội dung báo cáo không được vượt quá 2000 ký tự")
    private String description;

    @Size(max = 5, message = "Tối đa 5 bằng chứng cho mỗi báo cáo")
    @Builder.Default
    private List<@Size(max = 500, message = "URL bằng chứng không được vượt quá 500 ký tự") String> evidenceUrls =
            new ArrayList<>();
}
