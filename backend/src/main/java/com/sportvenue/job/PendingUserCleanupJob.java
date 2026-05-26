package com.sportvenue.job;

import com.sportvenue.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Job định kỳ dọn dẹp các tài khoản chưa xác thực quá 24h.
 */
@Component
@Slf4j
public class PendingUserCleanupJob {

    private final UserRepository userRepository;

    public PendingUserCleanupJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Chạy lúc 2 giờ sáng mỗi ngày.
     * Xóa các user chưa verify (isVerified = false) và đã tạo quá 24h.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupPendingUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        log.info("Starting cleanup of unverified users created before {}", threshold);
        
        try {
            userRepository.deleteAllByIsVerifiedFalseAndCreatedAtBefore(threshold);
            log.info("Cleanup of pending users completed successfully");
        } catch (Exception e) {
            log.error("Error occurred during pending user cleanup", e);
        }
    }
}
