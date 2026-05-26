package com.sportvenue.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure 6-digit OTP codes.
 */
@Component
public class OtpGenerator {

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        int otp = RANDOM.nextInt(1_000_000);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}
