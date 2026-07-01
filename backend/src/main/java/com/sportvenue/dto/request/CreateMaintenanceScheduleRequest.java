package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaintenanceScheduleRequest {

    @NotNull(message = "Ngày bắt đầu bảo trì không được để trống")
    private LocalDate startDate;

    /** NULL = bảo trì vô thời hạn. */
    private LocalDate endDate;

    @Size(max = 255, message = "Lý do bảo trì tối đa 255 ký tự")
    private String reason;
}
