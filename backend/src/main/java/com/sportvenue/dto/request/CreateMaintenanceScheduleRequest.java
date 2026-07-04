package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

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

    /** NULL = tính từ đầu ngày startDate (00:00). */
    private LocalTime startTime;

    /** NULL = tính đến hết ngày endDate (23:59:59). Chỉ hợp lệ khi endDate khác NULL. */
    private LocalTime endTime;

    @Size(max = 255, message = "Lý do bảo trì tối đa 255 ký tự")
    private String reason;
}
