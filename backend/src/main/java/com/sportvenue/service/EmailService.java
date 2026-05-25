package com.sportvenue.service;

import com.sportvenue.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Reusable email service — dùng chung cho OTP, thông báo booking, v.v.
 */
@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.mail.from:noreply@sportvenue.com}")
    private String fromAddress;

    /** Dev: true = in OTP ra log, không gọi SMTP. */
    @Value("${app.mail.mock:false}")
    private boolean mockMail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @PostConstruct
    void logMailConfiguration() {
        if (mockMail) {
            LOG.warn("app.mail.mock=true — OTP will be logged here only, not sent via SMTP");
            return;
        }
        if (!StringUtils.hasText(smtpUsername)) {
            LOG.warn("MAIL_USERNAME is empty — OTP emails will fail. Set SMTP in backend/.env or MAIL_MOCK=true");
        } else {
            LOG.info("SMTP enabled: from={} via host user={}", fromAddress, smtpUsername);
        }
    }

    /**
     * Gửi email OTP xác thực tài khoản sau đăng ký.
     */
    public void sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
        if (mockMail) {
            LOG.warn("=== DEV MAIL MOCK === OTP for {}: {} (expires in {} min) ===", toEmail, otpCode, expiryMinutes);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("SportVenue — Mã OTP xác thực đăng ký");
            helper.setText(buildOtpEmailBody(otpCode, expiryMinutes), true);
            mailSender.send(mimeMessage);
            LOG.info("OTP email sent to {}", toEmail);
        } catch (MailException | MessagingException ex) {
            LOG.error(
                    "Failed to send OTP to {}. OTP (dev fallback): {} — fix backend/.env SMTP",
                    toEmail, otpCode, ex
            );
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
        if (mockMail) {
            LOG.warn("=== DEV MAIL MOCK === Reset OTP for {}: {} ===", toEmail, otp);
            return;
        }

        LOG.info("Preparing to send OTP reset password email to: {}", toEmail);
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
            LOG.info("OTP reset password email successfully sent to: {}", toEmail);
            } catch (MailException e) {
            LOG.error("Failed to send OTP reset password email to: {}, error: {}", toEmail, e.getMessage());
            }
    }

    private String buildOtpEmailBody(String otpCode, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Xác thực tài khoản SportVenue</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:36px 40px;text-align:center;">
                              <div style="display:inline-block;background:rgba(255,255,255,0.15);border-radius:10px;padding:10px 20px;">
                                <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">⚽ SportVenue</span>
                              </div>
                              <p style="color:rgba(255,255,255,0.85);margin:12px 0 0;font-size:14px;">Nền tảng đặt sân thể thao trực tuyến</p>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h2 style="margin:0 0 8px;color:#1e293b;font-size:22px;font-weight:600;">Xác thực tài khoản của bạn</h2>
                              <p style="margin:0 0 28px;color:#64748b;font-size:15px;line-height:1.6;">
                                Chào mừng bạn đến với SportVenue! Vui lòng sử dụng mã OTP bên dưới để hoàn tất đăng ký tài khoản.
                              </p>
                
                              <!-- OTP Box -->
                              <div style="background:#f0f7ff;border:2px dashed #2563eb;border-radius:10px;padding:28px;text-align:center;margin-bottom:28px;">
                                <p style="margin:0 0 8px;color:#64748b;font-size:13px;text-transform:uppercase;letter-spacing:1.5px;font-weight:600;">Mã xác thực OTP</p>
                                <div style="font-size:42px;font-weight:700;letter-spacing:12px;color:#2563eb;font-family:'Courier New',monospace;">%s</div>
                                <p style="margin:12px 0 0;color:#94a3b8;font-size:13px;">⏱ Mã có hiệu lực trong <strong style="color:#ef4444;">%d phút</strong></p>
                              </div>
                
                              <!-- Steps -->
                              <div style="background:#f8fafc;border-radius:8px;padding:20px 24px;margin-bottom:28px;">
                                <p style="margin:0 0 12px;color:#475569;font-size:13px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Hướng dẫn</p>
                                <ol style="margin:0;padding-left:20px;color:#64748b;font-size:14px;line-height:2;">
                                  <li>Quay lại trang xác thực trên SportVenue</li>
                                  <li>Nhập mã OTP gồm 6 chữ số ở trên</li>
                                  <li>Nhấn <strong>Xác thực</strong> để kích hoạt tài khoản</li>
                                </ol>
                              </div>
                
                              <p style="margin:0;color:#94a3b8;font-size:13px;line-height:1.6;border-top:1px solid #e2e8f0;padding-top:20px;">
                                🔒 Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Mã OTP sẽ tự động hết hạn và tài khoản sẽ không được tạo.
                              </p>
                            </td>
                          </tr>
                
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                              <p style="margin:0;color:#94a3b8;font-size:12px;">© 2026 SportVenue. Tất cả quyền được bảo lưu.</p>
                              <p style="margin:6px 0 0;color:#cbd5e1;font-size:11px;">Email này được gửi tự động, vui lòng không trả lời.</p>
                            </td>
                          </tr>
                
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(otpCode, expiryMinutes);
    }
}
