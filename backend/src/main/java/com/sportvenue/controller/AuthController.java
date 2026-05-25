package com.sportvenue.controller;

import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.RegisterRequest;
import com.sportvenue.dto.UserResponse;
import com.sportvenue.dto.ForgotPasswordRequest;
import com.sportvenue.dto.ResetPasswordRequest;
import com.sportvenue.entity.User;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints phục vụ Đăng nhập & Đăng xuất")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản khách hàng mới và trả về JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng Email/Mật khẩu", description = "Xác thực email và mật khẩu của người dùng, trả về JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập nhanh qua Google", description = "Đăng nhập bằng tài khoản Google. Đăng ký tự động nếu là lần đầu tiên")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Yêu cầu đính kèm Bearer JWT Token ở Header")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userPrincipal.getUser();
        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roleName(user.getRole().getRoleName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .userRank(user.getUserRank())
                .userPoint(user.getUserPoint())
                .accountStatus(user.getAccountStatus())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu khôi phục mật khẩu", description = "Gửi mã khôi phục mật khẩu qua email cho người dùng")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                Map.of("message", "Nếu email tồn tại trên hệ thống, một liên kết khôi phục mật khẩu đã được gửi đi."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu mới", description = "Sử dụng token được cung cấp để đặt mật khẩu mới cho tài khoản")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Mật khẩu của bạn đã được thay đổi thành công."));
    }
}
