package com.sportvenue.controller;

import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Quản lý thông báo tập trung")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của người dùng hiện tại (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Boolean unreadOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(
                userPrincipal.getUser().getUserId(), unreadOnly, pageable);
        
        return ResponseEntity.ok(ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(result)
                .build());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo chưa đọc của người dùng hiện tại")
    public ResponseEntity<ApiResponse<Long>> countUnread(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        long count = notificationService.countUnread(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.<Long>builder().result(count).build());
    }

    @PatchMapping("/mark-as-read")
    @Operation(summary = "Đánh dấu danh sách thông báo là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<Long> notificationIds) {
        
        notificationService.markAsRead(userPrincipal.getUser().getUserId(), notificationIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đánh dấu thông báo là đã đọc")
                .build());
    }

    @PatchMapping("/mark-all-as-read")
    @Operation(summary = "Đánh dấu tất cả thông báo của người dùng hiện tại là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        notificationService.markAllAsRead(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đánh dấu tất cả thông báo là đã đọc")
                .build());
    }
}
