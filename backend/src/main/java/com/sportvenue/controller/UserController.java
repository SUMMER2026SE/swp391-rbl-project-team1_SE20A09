package com.sportvenue.controller;

import com.sportvenue.dto.ChangePasswordRequest;
import com.sportvenue.dto.UserProfileResponse;
import com.sportvenue.dto.request.UpgradeToOwnerRequest;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.service.UserService;
import com.sportvenue.service.OwnerRegistrationService;
import com.sportvenue.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final OwnerRegistrationService ownerRegistrationService;

    /**
     * Retrieves the profile of the current logged-in user.
     *
     * @param userPrincipal authenticated user principal
     * @return 200 OK with UserProfileResponse DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Requires Bearer JWT Token in Header")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getMyProfile(userPrincipal.getUsername()));
    }

    /**
     * Thay đổi mật khẩu cho tài khoản đang đăng nhập.
     *
     * @param userPrincipal authenticated user principal
     * @param request change password body containing old and new passwords
     * @return 200 OK with success message
     */
    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Đổi mật khẩu khi đang đăng nhập. Yêu cầu Bearer JWT Token ở Header")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userPrincipal.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "Thay đổi mật khẩu thành công!"));
    }

    @PostMapping("/me/upgrade-to-owner")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Customer gửi yêu cầu nâng cấp tài khoản thành Chủ sân", description = "Tạo hồ sơ chủ sân ở trạng thái PENDING. Yêu cầu role Customer và Bearer JWT Token ở Header")
    public ResponseEntity<ApiResponse<OwnerDetailResponse>> upgradeAccount(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpgradeToOwnerRequest request) {
        OwnerDetailResponse response = ownerRegistrationService.upgradeCurrentCustomer(
                userPrincipal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.<OwnerDetailResponse>builder()
                .code(200)
                .message("Gửi hồ sơ nâng cấp thành công. Đang chờ Admin phê duyệt.")
                .result(response)
                .build());
    }

    @PatchMapping("/me/resubmit-owner")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
        summary = "Owner bị từ chối nộp lại hồ sơ",
        description = "Chỉ dành cho Owner đã bị Admin từ chối (REJECTED). Cập nhật thông tin và gửi lại hồ sơ phê duyệt."
    )
    public ResponseEntity<ApiResponse<OwnerDetailResponse>> resubmitOwnerProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpgradeToOwnerRequest request) {
        OwnerDetailResponse response = ownerRegistrationService.resubmitOwnerProfile(
                userPrincipal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.<OwnerDetailResponse>builder()
                .code(200)
                .message("Nộp lại hồ sơ thành công. Đang chờ Admin phê duyệt lại.")
                .result(response)
                .build());
    }

    @GetMapping("/me/owner-profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy hồ sơ chủ sân của tài khoản hiện tại", description = "Lấy thông tin và trạng thái phê duyệt của hồ sơ chủ sân gắn với tài khoản đang đăng nhập. Yêu cầu Bearer JWT Token ở Header")
    public ResponseEntity<ApiResponse<OwnerDetailResponse>> getOwnerProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        OwnerDetailResponse response = ownerRegistrationService.getOwnerProfileOfUser(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.<OwnerDetailResponse>builder()
                .code(200)
                .message(response != null ? "Lấy hồ sơ chủ sân thành công." : "Tài khoản chưa có hồ sơ chủ sân.")
                .result(response)
                .build());
    }
}
