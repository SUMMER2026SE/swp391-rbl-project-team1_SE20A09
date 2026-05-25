package com.sportvenue.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.RegisterRequest;
import com.sportvenue.dto.UserResponse;
import com.sportvenue.dto.ForgotPasswordRequest;
import com.sportvenue.dto.ResetPasswordRequest;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email này đã được sử dụng.");
        }

        Role customerRole = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Customer"));

        // Tách họ và tên
        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        String phoneNumber = generateUniquePhoneNumber();

        User user = User.builder()
                .email(request.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(customerRole)
                .accountStatus("Active")
                .userRank("Bronze")
                .userPoint(0)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        log.info("Attempting Google login — verifying ID token...");

        // === SECURITY: Verify Google ID Token ===
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.getIdToken());
        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String avatarUrl = (String) payload.get("picture");

        log.info("Google token verified for email: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("First time Google login, registering user: {}", email);
            // Create user
            Role customerRole = roleRepository.findByRoleName("Customer")
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Customer"));

            // Generate a unique dummy phone number
            String phoneNumber = generateUniquePhoneNumber();

            user = User.builder()
                    .email(email)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .avatarUrl(avatarUrl)
                    .phoneNumber(phoneNumber)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Secure dummy password
                    .role(customerRole)
                    .accountStatus("Active")
                    .userRank("Bronze")
                    .userPoint(0)
                    .build();

            user = userRepository.save(user);
        } else {
            if ("Blocked".equalsIgnoreCase(user.getAccountStatus())) {
                throw new BadRequestException("Tài khoản của bạn đã bị khóa.");
            }
            // Optional: update avatar if changed
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

    /**
     * Xác thực Google ID Token bằng GoogleIdTokenVerifier.
     * Email được lấy từ payload đã xác thực — KHÔNG tin vào client gửi lên.
     */
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
        // Fallback: dùng prefix G- + UUID ngắn để tránh xung đột
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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Received forgot password request for email: {}", email);

        // Security mitigation: Rate Limiting to prevent email flooding (limit 1 request per 5 minutes per email)
        String rateLimitKey = "forgot-password:rate-limit:" + email;
        Boolean isRateLimited = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(isRateLimited)) {
            log.warn("Forgot password request rate limited for email: {}", email);
            throw new BadRequestException("Vui lòng đợi 5 phút trước khi yêu cầu lại mã khôi phục.");
        }

        // Check if user exists
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Mitigate User Enumeration: Log but do not throw exception, return successfully to the client
            log.warn("Forgot password request for non-existent email: {}", email);
            // Still set a generic rate limit key to match response times and prevent timing attacks
            redisTemplate.opsForValue().set(rateLimitKey, "true", 5, TimeUnit.MINUTES);
            return;
        }

        if ("Blocked".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa. Không thể thực hiện khôi phục mật khẩu.");
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        log.info("Generated reset password OTP for {}: {}", email, otp);

        // Save OTP in Redis with a 5-minute expiration time
        String otpKey = "reset:otp:" + email;
        redisTemplate.opsForValue().set(otpKey, otp, 5, TimeUnit.MINUTES);

        // Set rate limit key for 5 minutes
        redisTemplate.opsForValue().set(rateLimitKey, "true", 5, TimeUnit.MINUTES);

        // Send email with OTP
        emailService.sendResetPasswordOtpEmail(email, otp);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otpKey = "reset:otp:" + email;

        // Retrieve OTP associated with email
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            log.warn("Invalid or expired reset password OTP attempt for email: {}", email);
            throw new BadRequestException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));

        if ("Blocked".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa.");
        }

        // Update password hash
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Successfully reset password for email: {}", email);

        // Delete OTP immediately to prevent replay attacks
        redisTemplate.delete(otpKey);

        // Remove rate limit key early on success
        String rateLimitKey = "forgot-password:rate-limit:" + email;
        redisTemplate.delete(rateLimitKey);
    }
}
