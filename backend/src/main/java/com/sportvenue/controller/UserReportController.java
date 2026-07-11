package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateReportRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.ReportResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "User Reports", description = "Báo cáo hành vi người dùng")
@SecurityRequirement(name = "bearerAuth")
public class UserReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Tạo báo cáo hành vi người dùng")
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reportService.createReport(request, userPrincipal.getUsername()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Xem các báo cáo do chính mình gửi")
    public ResponseEntity<PageResponse<ReportResponse>> getMyReports(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(reportService.getMyReports(userPrincipal.getUsername(), pageable)));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Xem chi tiết báo cáo do chính mình gửi")
    public ResponseEntity<ReportResponse> getMyReport(
            @PathVariable Integer reportId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reportService.getMyReport(reportId, userPrincipal.getUsername()));
    }
}
