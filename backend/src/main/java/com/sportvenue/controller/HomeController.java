package com.sportvenue.controller;

import com.sportvenue.dto.home.HomeDashboardResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Trang chủ người dùng đã đăng nhập")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/dashboard")
    @Operation(
            summary = "Dashboard trang chủ (đã đăng nhập)",
            description = "Lấy lịch sắp tới, sân yêu thích, gợi ý và thống kê từ database")
    public ResponseEntity<HomeDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(homeService.getDashboard(userPrincipal));
    }
}
