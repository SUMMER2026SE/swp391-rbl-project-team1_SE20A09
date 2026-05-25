package com.sportvenue.service;

import com.sportvenue.dto.ForgotPasswordRequest;
import com.sportvenue.dto.ResetPasswordRequest;
import com.sportvenue.dto.request.GoogleLoginRequest;
import com.sportvenue.dto.request.LoginRequest;
import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.AuthResponse;
import com.sportvenue.dto.response.MessageResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);

    AuthResponse verifyOtp(String email, String otpCode);

    MessageResponse resendOtp(String email);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
