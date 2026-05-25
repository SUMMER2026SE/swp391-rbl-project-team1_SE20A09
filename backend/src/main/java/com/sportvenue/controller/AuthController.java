package com.sportvenue.controller;

import com.sportvenue.dto.request.GoogleLoginRequest;
import com.sportvenue.dto.request.LoginRequest;
import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.AuthResponse;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.UserResponse;
import com.sportvenue.entity.User;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints phục vụ Đăng ký, Xác thực OTP & Đăng nhập")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới và gửi mã OTP qua email")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực OTP", description = "Xác thực mã OTP và kích hoạt tài khoản, trả về JWT token")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam String email, @RequestParam String otpCode) {
        return ResponseEntity.ok(authService.verifyOtp(email, otpCode));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP mới vào email")
    public ResponseEntity<MessageResponse> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOtp(email));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng Email/Mật khẩu", description = "Xác thực email và mật khẩu, trả về JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập bằng Google", description = "Đăng nhập nhanh qua Google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Yêu cầu Bearer JWT Token")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
}
