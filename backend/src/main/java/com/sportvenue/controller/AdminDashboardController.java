package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminDashboardResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Xem số liệu tổng quan hệ thống — chỉ dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    @Operation(
        summary = "Xem tổng quan dashboard Admin",
        description = """
            Lấy các thông tin thống kê số lượng người dùng, doanh thu, bookings, khiếu nại của hệ thống.
            Nếu truyền startDate và endDate (định dạng yyyy-MM-dd), kết quả sẽ được lọc theo khoảng thời gian đó.
            Nếu không truyền, trả về số liệu tổng hợp từ đầu hệ thống.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Lấy dữ liệu dashboard thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Không có quyền — yêu cầu role Admin")
    })
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AdminDashboardResponse data;
        if (startDate != null && endDate != null) {
            data = adminDashboardService.getDashboardData(startDate, endDate);
        } else {
            data = adminDashboardService.getDashboardData();
        }

        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .code(200)
                .message("Lấy dữ liệu dashboard thành công")
                .result(data)
                .build());
    }
}
