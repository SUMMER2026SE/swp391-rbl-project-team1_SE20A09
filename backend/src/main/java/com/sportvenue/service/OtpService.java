package com.sportvenue.service;

import com.sportvenue.entity.OtpToken;
import com.sportvenue.entity.User;
import com.sportvenue.exception.EmailDeliveryException;
import com.sportvenue.repository.OtpTokenRepository;
import com.sportvenue.util.OtpGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.UserRepository;

/**
 * Service quản lý OTP — tạo, lưu và kiểm tra hết hạn.
 */
@Service
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;

    public OtpService(OtpTokenRepository otpTokenRepository, UserRepository userRepository, OtpGenerator otpGenerator, EmailService emailService) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository = userRepository;
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
    }

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    /**
     * Xác thực mã OTP của người dùng.
     */
    @Transactional
    public void verify(String email, String otpCode) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OtpToken otpToken = otpTokenRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_NOT_FOUND));

        if (otpToken.getUsed()) {
            throw new AppException(ErrorCode.OTP_ALREADY_USED);
        }

        if (otpToken.isExpired()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!otpToken.getOtpCode().equals(otpCode)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Success
        otpToken.setUsed(true);
        user.setIsVerified(true);
        user.setAccountStatus(com.sportvenue.entity.enums.AccountStatus.ACTIVE);
        
        otpTokenRepository.save(otpToken);
        userRepository.save(user);
        
        log.info("User {} verified successfully via OTP", email);
    }

    /**
     * Tạo OTP mới cho user, xóa OTP cũ, lưu DB và gửi email.
     */
    @Transactional
    public OtpToken createAndSendOtp(User user) {
        otpTokenRepository.deleteAllByUser(user);

        String otpCode = otpGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        OtpToken otpToken = new OtpToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(otpCode);
        otpToken.setExpiresAt(expiresAt);
        otpToken.setUsed(false);

        OtpToken saved = otpTokenRepository.save(otpToken);
        try {
            emailService.sendOtpEmail(user.getEmail(), otpCode, expiryMinutes);
        } catch (EmailDeliveryException ex) {
            // OTP đã lưu DB — không rollback đăng ký vì lỗi SMTP
            log.warn("OTP saved for {} but email failed. Code: {} — check logs or resend later.",
                    user.getEmail(), otpCode, ex);
        }
        return saved;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }
}
