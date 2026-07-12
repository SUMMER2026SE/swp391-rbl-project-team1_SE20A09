package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;
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
@Tag(name = "Admin Moderation Analytics", description = "Thong ke tong hop Report va Complaint cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminModerationAnalyticsController {

    private final AdminModerationAnalyticsService adminModerationAnalyticsService;

    @GetMapping
    @Operation(summary = "Admin xem thong ke Report va Complaint")
    public ResponseEntity<AdminModerationAnalyticsResponse> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") int topLimit) {
        return ResponseEntity.ok(adminModerationAnalyticsService.getAnalytics(
                from,
                to,
                role,
                category,
                status,
                topLimit));
    }
}
