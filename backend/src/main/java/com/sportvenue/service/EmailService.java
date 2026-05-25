package com.sportvenue.service;

public interface EmailService {
    void sendResetPasswordOtpEmail(String toEmail, String otp);
}
