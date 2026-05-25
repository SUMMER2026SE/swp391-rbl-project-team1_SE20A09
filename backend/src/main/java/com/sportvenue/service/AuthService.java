package com.sportvenue.service;

import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);
}
