package com.sportvenue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:onboarding@resend.dev}")
    private String fromEmail;

    @Override
    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        log.info("Preparing to send OTP reset password email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
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
        } catch (Exception e) {
            log.error("Failed to send OTP reset password email to: {}, error: {}", toEmail, e.getMessage());
        }
    }
}
