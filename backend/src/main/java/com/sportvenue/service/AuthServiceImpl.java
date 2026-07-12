package com.sportvenue.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sportvenue.dto.ForgotPasswordRequest;
import com.sportvenue.dto.ResetPasswordRequest;
import com.sportvenue.dto.UpdateProfileRequest;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.dto.request.GoogleLoginRequest;
import com.sportvenue.dto.request.LoginRequest;
import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.AuthResponse;
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
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.UserRank;
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
import com.sportvenue.util.AfterCommitExecutor;

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
    private final OtpService otpService;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final AfterCommitExecutor afterCommitExecutor;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Attempting registration for email: {}", email);

        if (userRepository.existsByEmail(email)) {
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
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(customerRole)
                .accountStatus(AccountStatus.PENDING)
                .isVerified(false)
                .userRank(UserRank.BRONZE)
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
        String normalizedEmail = email.trim().toLowerCase();
        otpService.verify(normalizedEmail, otpCode);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail());
        
        if ("Customer".equals(user.getRole().getRoleName())) {
            afterCommitExecutor.execute(() -> 
                emailService.sendCustomerRegistrationSuccessEmail(user.getEmail(), user.getFullName())
            );
        }
        
        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse resendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (user.getIsVerified()) {
            throw new BadRequestException("Tài khoản đã được xác thực.");
        }

        otpService.createAndSendOtp(user);
        return new MessageResponse("Mã OTP mới đã được gửi vào email của bạn.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Attempting login for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!user.getIsVerified()) {
            throw new AppException(ErrorCode.USER_NOT_VERIFIED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()));

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
                    .accountStatus(AccountStatus.ACTIVE)
                    .isVerified(true) // Google users are verified by default
                    .userRank(UserRank.BRONZE)
                    .userPoint(0)
                    .build();

            user = userRepository.save(user);
        } else {
            boolean updated = false;
            if (!user.getIsVerified()) {
                user.setIsVerified(true);
                user.setAccountStatus(AccountStatus.ACTIVE);
                updated = true;
                log.info("Verified previously unverified user via Google login: {}", email);
            }
            if (avatarUrl != null) {
                String currentAvatar = user.getAvatarUrl();
                if (currentAvatar == null || currentAvatar.isBlank() || currentAvatar.contains("googleusercontent.com")) {
                    if (!avatarUrl.equals(currentAvatar)) {
                        user.setAvatarUrl(avatarUrl);
                        updated = true;
                    }
                }
            }
            if (updated) {
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
        if (googleClientId == null || googleClientId.isBlank()) {
            log.error("Google Client ID chưa được cấu hình — kiểm tra biến GOOGLE_CLIENT_ID trong .env");
            throw new BadRequestException("Tính năng đăng nhập Google chưa được cấu hình trên server.");
        }

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
        } catch (BadRequestException e) {
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Lỗi xác thực Google ID Token (security/IO): {}", e.getMessage());
            throw new BadRequestException("Không thể xác thực Google ID Token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định khi verify Google token [{}: {}]", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BadRequestException("Xác thực Google thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal userPrincipal) {
        User user = requireAuthenticatedUser(userPrincipal);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UserPrincipal userPrincipal, UpdateProfileRequest request) {
        User user = requireAuthenticatedUser(userPrincipal);

        String phoneNumber = request.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumberAndUserIdNot(phoneNumber, user.getUserId())) {
            throw new BadRequestException("Số điện thoại này đã được sử dụng.");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        user.setPhoneNumber(phoneNumber);

        String avatarUrl = request.getAvatarUrl();
        if (avatarUrl != null) {
            String trimmed = avatarUrl.trim();
            user.setAvatarUrl(trimmed.isEmpty() ? null : trimmed);
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return mapToUserResponse(user);
    }

    private User requireAuthenticatedUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new BadRequestException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }
        return userPrincipal.getUser();
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
                .userRank(user.getUserRank() != null ? user.getUserRank().name() : null)
                .userPoint(user.getUserPoint())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .lockReason(user.getLockReason())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Received forgot password request for email: {}", email);

        // Security mitigation: Rate Limiting to prevent email flooding (limit 1 request
        // per 2 minutes per email)
        String rateLimitKey = "forgot-password:rate-limit:" + email;
        Boolean isRateLimited = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(isRateLimited)) {
            log.warn("Forgot password request rate limited for email: {}", email);
            throw new BadRequestException("Vui lòng đợi 2 phút trước khi yêu cầu lại mã khôi phục.");
        }

        // Check if user exists
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Mitigate User Enumeration: Log but do not throw exception, return
            // successfully to the client
            log.warn("Forgot password request for non-existent email: {}", email);
            // Still set a generic rate limit key to match response times and prevent timing
            // attacks
            redisTemplate.opsForValue().set(rateLimitKey, "true", 2, TimeUnit.MINUTES);
            return;
        }

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa. Không thể thực hiện khôi phục mật khẩu.");
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        log.info("Generated reset password OTP for {}: {}", email, otp);

        // Save OTP in Redis with a 5-minute expiration time
        String otpKey = "reset:otp:" + email;
        redisTemplate.opsForValue().set(otpKey, otp, 5, TimeUnit.MINUTES);

        // Set rate limit key for 2 minutes
        redisTemplate.opsForValue().set(rateLimitKey, "true", 2, TimeUnit.MINUTES);

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

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
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
