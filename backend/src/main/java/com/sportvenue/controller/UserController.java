package com.sportvenue.controller;

import com.sportvenue.dto.ChangePasswordRequest;
import com.sportvenue.dto.UserProfileResponse;
import com.sportvenue.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.sportvenue.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
