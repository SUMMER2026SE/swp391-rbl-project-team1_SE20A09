package com.sportvenue.service;

import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.RegisterRequest;
import com.sportvenue.dto.UpdateProfileRequest;
import com.sportvenue.dto.UserResponse;
import com.sportvenue.security.UserPrincipal;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    UserResponse getCurrentUser(UserPrincipal userPrincipal);

    UserResponse updateProfile(UserPrincipal userPrincipal, UpdateProfileRequest request);
}
