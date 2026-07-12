package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateAppealRequest;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.AppealResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
@Tag(name = "Appeals", description = "Kháng cáo mở khóa tài khoản")
@SecurityRequirement(name = "bearerAuth")
public class AppealController {

    private final AppealService appealService;

    @PostMapping
    @Operation(summary = "Gửi kháng cáo mở khóa tài khoản")
    public ResponseEntity<ApiResponse<AppealResponse>> createAppeal(
            @Valid @RequestBody CreateAppealRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        AppealResponse response = appealService.createAppeal(request, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AppealResponse>builder()
                .message("Gửi kháng cáo thành công.")
                .result(response)
                .build());
    }

    @GetMapping("/me")
    @Operation(summary = "Xem kháng cáo gần nhất của tài khoản hiện tại")
    public ResponseEntity<ApiResponse<AppealResponse>> getMyLatestAppeal(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.<AppealResponse>builder()
                .result(appealService.getMyLatestAppeal(userPrincipal))
                .build());
    }
}
