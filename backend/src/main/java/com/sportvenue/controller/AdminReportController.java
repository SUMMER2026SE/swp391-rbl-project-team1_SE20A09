package com.sportvenue.controller;

import com.sportvenue.dto.request.ResolveReportRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.ReportResponse;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Admin xử lý báo cáo hành vi người dùng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "Admin xem danh sách báo cáo hành vi")
    public ResponseEntity<PageResponse<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(reportService.getAdminReports(status, category, pageable)));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Admin xem chi tiết báo cáo hành vi")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Integer reportId) {
        return ResponseEntity.ok(reportService.getAdminReport(reportId));
    }

    @PatchMapping("/{reportId}")
    @Operation(summary = "Admin cập nhật trạng thái xử lý báo cáo")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable Integer reportId,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                reportService.updateReportStatus(reportId, request, userPrincipal.getUsername()));
    }
}
