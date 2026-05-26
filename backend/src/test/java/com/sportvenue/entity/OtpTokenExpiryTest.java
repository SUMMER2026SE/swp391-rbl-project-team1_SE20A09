package com.sportvenue.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OtpTokenExpiryTest {

    @Test
    void shouldNotBeExpiredWhenWithinFiveMinutes() {
        OtpToken token = new OtpToken();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void shouldBeExpiredWhenPastExpiryTime() {
        OtpToken token = new OtpToken();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void shouldBeExpiredExactlyAtExpiryBoundary() {
        OtpToken token = new OtpToken();
        token.setExpiresAt(LocalDateTime.now().minusNanos(1));

        assertThat(token.isExpired()).isTrue();
    }
}
