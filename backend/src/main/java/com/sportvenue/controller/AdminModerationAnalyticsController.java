package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import com.sportvenue.service.AdminModerationAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/admin/moderation-analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Moderation Analytics", description = "Thống kê tổng hợp Report và Complaint cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminModerationAnalyticsController {

    private final AdminModerationAnalyticsService adminModerationAnalyticsService;

    @GetMapping
    @Operation(
            summary = "Admin xem thống kê Report và Complaint",
            description = "reportCategory/reportStatus chỉ lọc Report, complaintPriority/complaintStatus chỉ lọc "
                    + "Complaint — 2 domain lọc độc lập, không loại trừ lẫn nhau."
    )
    public ResponseEntity<AdminModerationAnalyticsResponse> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) ReportCategory reportCategory,
            @RequestParam(required = false) ComplaintPriority complaintPriority,
            @RequestParam(required = false) ReportStatus reportStatus,
            @RequestParam(required = false) ComplaintStatus complaintStatus,
            @RequestParam(defaultValue = "10") int topLimit) {
        return ResponseEntity.ok(adminModerationAnalyticsService.getAnalytics(
                from,
                to,
                role,
                reportCategory,
                complaintPriority,
                reportStatus,
                complaintStatus,
                topLimit));
    }
}
