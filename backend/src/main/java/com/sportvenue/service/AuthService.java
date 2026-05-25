package com.sportvenue.service;

import com.sportvenue.dto.request.RegisterRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.exception.DuplicateResourceException;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication business logic — UC-AUTH-01 Register.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String CUSTOMER_ROLE = "Customer";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, OtpService otpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        validateUniqueUser(request);

        Role customerRole = roleRepository.findByRoleName(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + CUSTOMER_ROLE));

        NameParts nameParts = splitName(request.getName());
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setRole(customerRole);
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setPhoneNumber(request.getPhone());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(hashedPassword);
        user.setIsVerified(false);
        user.setAccountStatus("Pending");

        User savedUser = userRepository.save(user);
        otpService.createAndSendOtp(savedUser);

        log.info("User registered: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());
        return new MessageResponse(
                "Registration successful. Please check your email for the OTP verification code."
        );
    }

    private void validateUniqueUser(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new DuplicateResourceException("Phone number is already registered");
        }
    }

    private NameParts splitName(String fullName) {
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex < 0) {
            return new NameParts(trimmed, trimmed);
        }
        return new NameParts(
                trimmed.substring(0, spaceIndex),
                trimmed.substring(spaceIndex + 1).trim()
        );
    }

    private record NameParts(String firstName, String lastName) {
    }
}
