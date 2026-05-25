package com.sportvenue.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpGeneratorTest {

    private final OtpGenerator otpGenerator = new OtpGenerator();

    @RepeatedTest(20)
    void shouldGenerateSixDigitNumericOtp() {
        String otp = otpGenerator.generate();

        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void shouldPadLeadingZeros() {
        // SecureRandom output varies; verify format always produces 6 chars
        for (int i = 0; i < 100; i++) {
            assertThat(otpGenerator.generate()).hasSize(6);
        }
    }
}
