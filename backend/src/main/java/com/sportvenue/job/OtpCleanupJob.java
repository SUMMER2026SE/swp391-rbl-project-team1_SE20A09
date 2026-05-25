package com.sportvenue.job;

import com.sportvenue.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Job dọn dẹp mã OTP đã hết hạn trong DB.
 */
@Component
@RequiredArgsConstructor
public class OtpCleanupJob {

    private static final Logger LOG = LoggerFactory.getLogger(OtpCleanupJob.class);

    private final OtpTokenRepository otpTokenRepository;

    /**
     * Chạy mỗi giờ một lần để xóa OTP hết hạn.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredOtps() {
        LOG.info("Starting cleanup of expired OTP tokens...");
        try {
            otpTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            LOG.info("Expired OTP tokens cleaned up successfully.");
        } catch (Exception e) {
            LOG.error("Error cleaning up expired OTPs: {}", e.getMessage());
        }
    }
}
