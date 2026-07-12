package com.sportvenue.controller;

import com.sportvenue.dto.request.ReviewAppealRequest;
import com.sportvenue.dto.response.AppealResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AppealStatus;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AppealService;
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
@RequestMapping("/api/v1/admin/appeals")
@RequiredArgsConstructor
@Tag(name = "Admin Appeals", description = "Admin xử lý kháng cáo mở khóa tài khoản")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminAppealController {

    private final AppealService appealService;

    @GetMapping
    @Operation(summary = "Admin xem danh sách kháng cáo")
    public ResponseEntity<PageResponse<AppealResponse>> getAppeals(
            @RequestParam(required = false) AppealStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(appealService.getAppeals(status, pageable)));
    }

    @PatchMapping("/{appealId}/review")
    @Operation(summary = "Admin duyệt hoặc từ chối kháng cáo")
    public ResponseEntity<AppealResponse> reviewAppeal(
            @PathVariable Integer appealId,
            @Valid @RequestBody ReviewAppealRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appealService.reviewAppeal(appealId, request, userPrincipal));
    }
}
