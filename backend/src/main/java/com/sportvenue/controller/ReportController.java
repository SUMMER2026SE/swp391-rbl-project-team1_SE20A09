package com.sportvenue.controller;

import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
public class ReportController {

    private final RevenueService revenueService;

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('Owner')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer stadiumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
            
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        RevenueReportResponse response = revenueService.getRevenueReport(
                userDetails.getUsername(), stadiumId, startDateTime, endDateTime);

        return ResponseEntity.ok(ApiResponse.<RevenueReportResponse>builder()
                .code(200)
                .message("Lấy báo cáo doanh thu thành công")
                .result(response)
                .build());
    }
}
