package com.sportvenue.service;

import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.UserResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);

    AuthResponse verifyOtp(String email, String otpCode);

    MessageResponse resendOtp(String email);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);
}
