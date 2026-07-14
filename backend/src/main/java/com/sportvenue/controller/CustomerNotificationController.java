package com.sportvenue.controller;

import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/notifications")
@RequiredArgsConstructor
@Tag(name = "Customer Notification", description = "Customer Notification Management APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Customer')")
public class CustomerNotificationController {

    private final CustomerNotificationService customerNotificationService;

    @Operation(summary = "Get list of customer notifications with pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = customerNotificationService.getNotifications(userPrincipal.getUser().getUserId(), unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<NotificationResponse>>builder().code(200).message("Success").result(response).build());
    }

    @Operation(summary = "Get unread count for customer")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        long count = customerNotificationService.countUnread(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.<Long>builder().code(200).message("Success").result(count).build());
    }

    @Operation(summary = "Get recent notifications for customer")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "5") int limit) {
        List<NotificationResponse> response = customerNotificationService.getRecentNotifications(userPrincipal.getUser().getUserId(), limit);
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder().code(200).message("Success").result(response).build());
    }

    @Operation(summary = "Mark specific notifications as read")
    @PatchMapping("/mark-as-read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<Long> notificationIds) {
        customerNotificationService.markAsRead(userPrincipal.getUser().getUserId(), notificationIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder().code(200).message("Success").build());
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        customerNotificationService.markAllAsRead(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.<Void>builder().code(200).message("Success").build());
    }
}
