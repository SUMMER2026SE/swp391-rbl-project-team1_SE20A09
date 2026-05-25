package com.sportvenue.job;

import com.sportvenue.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Job dọn dẹp mã OTP đã hết hạn trong DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupJob {

    private final OtpTokenRepository otpTokenRepository;

    /**
     * Chạy mỗi giờ một lần để xóa OTP hết hạn.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredOtps() {
        log.info("Starting cleanup of expired OTP tokens...");
        try {
            otpTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired OTP tokens cleaned up successfully.");
        } catch (Exception e) {
            log.error("Error cleaning up expired OTPs: {}", e.getMessage());
        }
    }
}
