package com.sportvenue.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sportvenue.dto.response.AuthResponse;
import com.sportvenue.dto.request.GoogleLoginRequest;
import com.sportvenue.dto.request.LoginRequest;
import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.UserResponse;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        log.info("Attempting registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email này đã được sử dụng.");
        }
        if (userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new BadRequestException("Số điện thoại này đã được sử dụng.");
        }

        Role customerRole = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Customer"));

        // Tách họ và tên
        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(customerRole)
                .accountStatus("Pending")
                .isVerified(false)
                .userRank("Bronze")
                .userPoint(0)
                .build();

        user = userRepository.save(user);
        otpService.createAndSendOtp(user);

        log.info("User registered successfully (pending verification): {}", user.getEmail());
        return new MessageResponse("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã xác thực OTP.");
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(String email, String otpCode) {
        otpService.verify(email, otpCode);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail());
        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        
        if (user.getIsVerified()) {
            throw new BadRequestException("Tài khoản đã được xác thực.");
        }

        otpService.createAndSendOtp(user);
        return new MessageResponse("Mã OTP mới đã được gửi vào email của bạn.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!user.getIsVerified()) {
            throw new AppException(ErrorCode.USER_NOT_VERIFIED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        log.info("Attempting Google login — verifying ID token...");

        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.getIdToken());
        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String avatarUrl = (String) payload.get("picture");

        log.info("Google token verified for email: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("First time Google login, registering user: {}", email);
            Role customerRole = roleRepository.findByRoleName("Customer")
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Customer"));

            String phoneNumber = generateUniquePhoneNumber();

            user = User.builder()
                    .email(email)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .avatarUrl(avatarUrl)
                    .phoneNumber(phoneNumber)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(customerRole)
                    .accountStatus("Active")
                    .isVerified(true) // Google users are verified by default
                    .userRank("Bronze")
                    .userPoint(0)
                    .build();

            user = userRepository.save(user);
        } else {
            if ("Blocked".equalsIgnoreCase(user.getAccountStatus())) {
                throw new BadRequestException("Tài khoản của bạn đã bị khóa.");
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                user = userRepository.save(user);
            }
        }

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadRequestException("Google ID Token không hợp lệ hoặc đã hết hạn.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadRequestException("Email Google chưa được xác thực.");
            }

            return payload;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Lỗi xác thực Google ID Token: {}", e.getMessage());
            throw new BadRequestException("Không thể xác thực Google ID Token: " + e.getMessage());
        }
    }

    private String generateUniquePhoneNumber() {
        int attempts = 0;
        while (attempts < 10) {
            String phoneNumber = "09" + String.format("%08d",
                    ThreadLocalRandom.current().nextInt(0, 100_000_000));
            if (!userRepository.existsByPhoneNumber(phoneNumber)) {
                return phoneNumber;
            }
            attempts++;
        }
        return "G-" + UUID.randomUUID().toString().substring(0, 10);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
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
    }
}
