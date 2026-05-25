package com.sportvenue.service;

import com.sportvenue.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Reusable email service — dùng chung cho OTP, thông báo booking, v.v.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.mail.from:noreply@sportvenue.com}")
    private String fromAddress;

    /** Dev: true = in OTP ra log, không gọi SMTP. */
    @Value("${app.mail.mock:false}")
    private boolean mockMail;

    /**
     * Gửi email OTP xác thực tài khoản sau đăng ký.
     */
    public void sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
        if (mockMail) {
            log.warn("=== DEV MAIL MOCK === OTP for {}: {} (expires in {} min) ===", toEmail, otpCode, expiryMinutes);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Sport Venue — Email Verification OTP");
        message.setText(buildOtpEmailBody(otpCode, expiryMinutes));

        try {
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}. OTP code: {}", toEmail, otpCode, ex);
            throw new EmailDeliveryException(
                    "Could not send verification email. Please check mail configuration or try again later.",
                    ex
            );
        }
    }

    /**
     * Gửi email OTP khôi phục mật khẩu.
     */
    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        log.info("Preparing to send OTP reset password email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("🔑 Mã OTP Khôi Phục Mật Khẩu — SportVenue");

            String textContent = "Xin chào,\n\n" +
                    "Bạn đã yêu cầu khôi phục mật khẩu cho tài khoản SportVenue.\n" +
                    "Mã OTP của bạn là: " + otp + "\n\n" +
                    "Mã OTP này có hiệu lực trong vòng 5 phút. Vui lòng nhập mã này vào trang khôi phục để đặt lại mật khẩu mới.\n\n" +
                    "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.";

            message.setText(textContent);

            mailSender.send(message);
            log.info("OTP reset password email successfully sent to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send OTP reset password email to: {}, error: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailBody(String otpCode, int expiryMinutes) {
        return """
                Hello,

                Your Sport Venue verification code is: %s

                This code expires in %d minutes.

                If you did not register, please ignore this email.

                — Sport Venue Team
                """.formatted(otpCode, expiryMinutes);
    }
}
