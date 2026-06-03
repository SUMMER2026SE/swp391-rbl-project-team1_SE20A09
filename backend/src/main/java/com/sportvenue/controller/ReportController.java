package com.sportvenue.controller;

import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.sportvenue.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/owner/reports")
@RequiredArgsConstructor
@Tag(name = "Revenue Report", description = "Báo cáo doanh thu chi tiết theo ngày và từng sân — UC-OWN-10")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final RevenueService revenueService;

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
        summary = "Xem báo cáo doanh thu",
        description = "Trả về báo cáo doanh thu theo ngày và breakdown theo từng sân. "
                    + "Owner chỉ xem được dữ liệu của chính mình. "
                    + "Khoảng thời gian tối đa: 365 ngày."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy báo cáo thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "startDate phải trước endDate hoặc range vượt quá 365 ngày"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền — yêu cầu role Owner")
    })
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "ID sân cụ thể muốn lọc. Bỏ trống = tất cả sân của Owner.")
            @RequestParam(required = false) Integer stadiumId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", required = true, example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", required = true, example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // RULE-05: Validate tại controller — fail-fast trước khi vào service
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<RevenueReportResponse>builder()
                    .code(400)
                    .message("startDate phải trước hoặc bằng endDate")
                    .build());
        }

        if (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<RevenueReportResponse>builder()
                    .code(400)
                    .message("Khoảng thời gian báo cáo không được vượt quá 365 ngày")
                    .build());
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        RevenueReportResponse response = revenueService.getRevenueReport(
                userPrincipal.getUsername(), stadiumId, startDateTime, endDateTime);

        return ResponseEntity.ok(ApiResponse.<RevenueReportResponse>builder()
                .code(200)
                .message("Lấy báo cáo doanh thu thành công")
                .result(response)
                .build());
    }
}
