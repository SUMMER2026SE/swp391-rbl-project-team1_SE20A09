package com.sportvenue.config;

import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeder chạy khi khởi động — đảm bảo các tài khoản seed
 * luôn có mật khẩu BCrypt hợp lệ (do PasswordEncoder sinh ra).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SEED_PASSWORD = "password123";

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin@sportvenue.com", "Admin", "System", "0900000001", "Admin");
        seedUser("owner@sportvenue.com", "Hoàng", "Mai Huy", "0900000002", "Owner");
        seedUser("cun030206@gmail.com", "Hoàng", "Mai Huy", "0912345678", "Customer");
        log.info("✅ DataSeeder: Seed accounts verified/updated.");
    }

    private void seedUser(String email, String firstName, String lastName,
                          String phone, String roleName) {
        var optUser = userRepository.findByEmail(email);
        String encodedPassword = passwordEncoder.encode(SEED_PASSWORD);

        if (optUser.isPresent()) {
            User user = optUser.get();
            // Luôn cập nhật lại password hash để đảm bảo khớp với SEED_PASSWORD
            if (!passwordEncoder.matches(SEED_PASSWORD, user.getPasswordHash())) {
                user.setPasswordHash(encodedPassword);
                userRepository.save(user);
                log.info("🔑 Updated password for seed user: {}", email);
            }
        } else {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

            User user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .passwordHash(encodedPassword)
                    .role(role)
                    .accountStatus("Active")
                    .userRank("Bronze")
                    .userPoint(0)
                    .build();
            userRepository.save(user);
            log.info("➕ Created seed user: {}", email);
        }
    }
}
