package com.sportvenue.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void shouldHashPasswordWithBcrypt() {
        String rawPassword = "SecureP@ss1";

        String hashed = passwordEncoder.encode(rawPassword);

        assertThat(hashed).isNotEqualTo(rawPassword);
        assertThat(hashed).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, hashed)).isTrue();
    }

    @Test
    void shouldRejectWrongPassword() {
        String hashed = passwordEncoder.encode("CorrectP@ss1");

        assertThat(passwordEncoder.matches("WrongP@ss1", hashed)).isFalse();
    }

    @Test
    void shouldProduceDifferentHashesForSamePassword() {
        String rawPassword = "SecureP@ss1";

        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
    }
}
