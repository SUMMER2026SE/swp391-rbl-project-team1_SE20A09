package com.sportvenue.controller;

import com.sportvenue.dto.request.MarkReadRequest;
import com.sportvenue.dto.response.AdminDashboardResponse;
import com.sportvenue.dto.response.AdminNotificationResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AdminDashboardService;
import com.sportvenue.service.AdminNotificationService;
import com.sportvenue.service.impl.AdminNotificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Xem số liệu tổng quan hệ thống — chỉ dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminNotificationService adminNotificationService;

    // ── Dashboard ──────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(
        summary = "Xem tổng quan dashboard Admin",
        description = """
            Lấy các thông tin thống kê số lượng người dùng, doanh thu, bookings, khiếu nại của hệ thống.
            Nếu truyền startDate và endDate (định dạng yyyy-MM-dd), kết quả sẽ được lọc theo khoảng thời gian đó.
            Nếu không truyền, trả về số liệu tổng hợp từ đầu hệ thống.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Lấy dữ liệu dashboard thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Không có quyền — yêu cầu role Admin")
    })
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AdminDashboardResponse data;
        if (startDate != null && endDate != null) {
            data = adminDashboardService.getDashboardData(startDate, endDate);
        } else {
            data = adminDashboardService.getDashboardData();
        }

        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .code(200)
                .message("Lấy dữ liệu dashboard thành công")
                .result(data)
                .build());
    }

    // ── Admin Notifications ────────────────────────────────────────────────

    @GetMapping("/notifications")
    @Operation(summary = "Lấy thông báo tổng hợp cho Admin (chủ sân mới, sân mới, khiếu nại)")
    public ResponseEntity<ApiResponse<List<AdminNotificationResponse>>> getAdminNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<AdminNotificationResponse> notifications = adminNotificationService
                .getAdminNotifications(userPrincipal.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.<List<AdminNotificationResponse>>builder()
                .code(200)
                .message("Lấy thông báo thành công")
                .result(notifications)
                .build());
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Đếm số thông báo chưa đọc của Admin")
    public ResponseEntity<ApiResponse<Long>> countUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        long count = adminNotificationService.countUnread(userPrincipal.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .result(count)
                .build());
    }

    @PatchMapping("/notifications/mark-as-read")
    @Operation(summary = "Đánh dấu một thông báo Admin là đã đọc")
    public ResponseEntity<ApiResponse<Long>> markNotificationAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody MarkReadRequest request) {

        Integer adminUserId = userPrincipal.getUser().getUserId();

        // Nếu đã có notificationId → update trực tiếp (nhanh, không cần lookup)
        if (request.getNotificationId() != null) {
            adminNotificationService.markAsRead(adminUserId, request.getNotificationId());
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .code(200).message("Đã đánh dấu đã đọc")
                    .result(request.getNotificationId()).build());
        }

        // Chưa có notificationId → tìm / tạo mới bằng resourceId
        Long notificationId = ((AdminNotificationServiceImpl) adminNotificationService)
                .markAsReadByResourceId(
                        adminUserId,
                        request.getResourceId(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getType());

        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200).message("Đã đánh dấu đã đọc")
                .result(notificationId).build());
    }

    @PatchMapping("/notifications/mark-all-as-read")
    @Operation(summary = "Đánh dấu tất cả thông báo Admin là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        adminNotificationService.markAllAsRead(userPrincipal.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Đã đánh dấu tất cả là đã đọc")
                .build());
    }
}
