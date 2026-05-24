package com.sportvenue.service;

import com.sportvenue.dto.AuthResponse;
import com.sportvenue.dto.GoogleLoginRequest;
import com.sportvenue.dto.LoginRequest;
import com.sportvenue.dto.RegisterRequest;
import com.sportvenue.dto.UserResponse;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
    @Transactional(readOnly = true)
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
        log.info("Attempting Google login for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.info("First time Google login, registering user: {}", request.getEmail());
            // Create user
            Role customerRole = roleRepository.findByRoleName("Customer")
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Customer"));

            // Generate a unique dummy phone number
            String phoneNumber = generateUniquePhoneNumber();

            user = User.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName() != null ? request.getLastName() : "")
                    .avatarUrl(request.getAvatarUrl())
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
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(request.getAvatarUrl());
                user = userRepository.save(user);
            }
        }

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(mapToUserResponse(user))
                .build();
    }

    private String generateUniquePhoneNumber() {
        int attempts = 0;
        while (attempts < 10) {
            String phoneNumber = "09" + String.format("%08d", (int) (Math.random() * 100000000));
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
