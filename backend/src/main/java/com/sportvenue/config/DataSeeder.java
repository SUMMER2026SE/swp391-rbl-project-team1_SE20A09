package com.sportvenue.config;

import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.AmenityRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OwnerRepository ownerRepository;
    private final SportTypeRepository sportTypeRepository;
    private final AmenityRepository amenityRepository;
    private final StadiumRepository stadiumRepository;

    private static final String SEED_PASSWORD = "password123";

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin@sportvenue.com", "Admin", "System", "0900000001", "Admin");
        User ownerUser = seedUser("owner@sportvenue.com", "Hoàng", "Mai Huy", "0900000002", "Owner");
        seedUser("customer@sportvenue.com", "Hoàng", "Mai Huy", "0912345678", "Customer");
        
        seedAmenitiesAndStadium(ownerUser);

        log.info("✅ DataSeeder: Seed accounts verified/updated.");
    }

    private User seedUser(String email, String firstName, String lastName,
                          String phone, String roleName) {
        var optUser = userRepository.findByEmail(email);
        String encodedPassword = passwordEncoder.encode(SEED_PASSWORD);

        if (optUser.isPresent()) {
            User user = optUser.get();
            if (!passwordEncoder.matches(SEED_PASSWORD, user.getPasswordHash())) {
                user.setPasswordHash(encodedPassword);
                userRepository.save(user);
                log.info("🔑 Updated password for seed user: {}", email);
            }
            return user;
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
                    .isVerified(true)
                    .build();
            userRepository.save(user);
            log.info("➕ Created seed user: {}", email);
            return user;
        }
    }

    private void seedAmenitiesAndStadium(User ownerUser) {
        Owner owner = ownerRepository.findById(1).orElseGet(() -> {
            Owner newOwner = Owner.builder()
                    .user(ownerUser)
                    .businessName("Công ty TNHH Sân Bóng")
                    .build();
            return ownerRepository.save(newOwner);
        });

        SportType football = sportTypeRepository.findBySportName("Football").orElseGet(() -> {
            return sportTypeRepository.save(SportType.builder().sportName("Football").build());
        });

        Amenity wifi = amenityRepository.findByName("Wifi").orElse(null);
        Amenity parking = amenityRepository.findByName("Bãi đỗ xe").orElse(null);

        if (wifi != null && parking != null && stadiumRepository.count() == 0) {
            Stadium stadium = Stadium.builder()
                    .owner(owner)
                    .sportType(football)
                    .stadiumName("Sân Bóng Thành Công")
                    .description("Sân bóng đá cỏ nhân tạo chất lượng cao")
                    .address("Quận 1, TP. HCM")
                    .pricePerHour(new BigDecimal("200000"))
                    .capacity(14)
                    .openTime(LocalTime.of(6, 0))
                    .closeTime(LocalTime.of(22, 0))
                    .latitude(10.7769)
                    .longitude(106.7009)
                    .amenities(List.of(wifi, parking))
                    .build();
            stadiumRepository.save(stadium);
            log.info("➕ Created seed stadium with amenities.");
        }
    }
}
