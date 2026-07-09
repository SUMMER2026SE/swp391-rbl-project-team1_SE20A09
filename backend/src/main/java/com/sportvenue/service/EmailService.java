package com.sportvenue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sportvenue.exception.EmailDeliveryException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


import com.sportvenue.entity.TimeSlot;

/**
 * Reusable email service — dùng chung cho OTP, thông báo booking, v.v.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.mail.from:noreply@sportvenue.com}")
    private String fromAddress;

    @Value("${app.mail.mock:false}")
    private boolean mockMail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    /**
     * Gửi email OTP xác thực tài khoản sau đăng ký.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
        if (mockMail) {
            log.warn("=== DEV MAIL MOCK === OTP for {}: {} (expires in {} min) ===", toEmail, otpCode, expiryMinutes);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("SportsBook — Mã OTP xác thực đăng ký");
            helper.setText(buildOtpEmailBody(otpCode, expiryMinutes), true);
            mailSender.send(mimeMessage);
            log.info("OTP email sent to {}", toEmail);
        } catch (MailException | MessagingException ex) {
            log.error(
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
    @Async
    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        if (mockMail) {
            log.warn("=== DEV MAIL MOCK === OTP Reset for {}: {} ===", toEmail, otp);
            return;
        }
        log.info("Preparing to send OTP reset password email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("🔑 Mã OTP Khôi Phục Mật Khẩu — SportsBook");

            String textContent = "Xin chào,\n\n" +
                    "Bạn đã yêu cầu khôi phục mật khẩu cho tài khoản SportsBook.\n" +
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

    /**
     * Gửi email thông báo khóa/mở khóa tài khoản chủ sân.
     */
    @Async
    public void sendAccountStatusNotification(String toEmail, String businessName, boolean isEnabled, String reason) {
        if (mockMail) {
            log.warn("=== DEV MAIL MOCK === Account Status for {}: isEnabled={}, reason={} ===", toEmail, isEnabled, reason);
            return;
        }
        log.info("Preparing to send account status notification email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Thông báo trạng thái tài khoản đối tác — SportsBook");

            String status = isEnabled ? "MỞ KHÓA" : "BỊ KHÓA";
            String textContent = "Xin chào " + businessName + ",\n\n" +
                    "Tài khoản đối tác của bạn trên hệ thống SportsBook vừa được " + status + ".\n\n";
            
            if (!isEnabled && StringUtils.hasText(reason)) {
                textContent += "Lý do khóa: " + reason + "\n\n";
            } else if (isEnabled && StringUtils.hasText(reason)) {
                textContent += "Ghi chú: " + reason + "\n\n";
            }

            if (!isEnabled) {
                textContent += "Khi tài khoản bị khóa, bạn sẽ không thể đăng nhập và toàn bộ sân thể thao của bạn sẽ tạm thời không nhận đặt lịch mới. Vui lòng liên hệ ban quản trị để biết thêm chi tiết.\n\n";
            } else {
                textContent += "Bạn đã có thể đăng nhập bình thường. Chúc bạn kinh doanh thuận lợi!\n\n";
            }

            textContent += "Trân trọng,\nBan quản trị SportsBook";

            message.setText(textContent);

            mailSender.send(message);
            log.info("Account status notification email successfully sent to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send account status notification email to: {}, error: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email nhắc lịch chơi sắp tới.
     */
    @Async
    public void sendBookingReminderEmail(String toEmail,
                                         String stadiumName,
                                         String reservationDate,
                                         String timeRange) {
        if (mockMail) {
            log.warn("=== DEV MAIL MOCK === Booking Reminder for {}: Stadium {}, Time {} {} ===", toEmail, stadiumName, reservationDate, timeRange);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("⏰ Nhắc lịch chơi thể thao - " + stadiumName);
            message.setText(
                    "Xin chào,\n\n" +
                    "Bạn có lịch đặt sân sắp tới:\n\n" +
                    "  🏟️  Sân:  " + stadiumName + "\n" +
                    "  📅  Ngày: " + reservationDate + "\n" +
                    "  🕐  Giờ:  " + timeRange + "\n\n" +
                    "Vui lòng có mặt đúng giờ. Nếu cần hỗ trợ, liên hệ với chúng tôi qua ứng dụng.\n" +
                    "Chúc bạn có buổi chơi thể thao vui vẻ!\n\n" +
                    "Đội ngũ SportsBook"
            );
            mailSender.send(message);
            log.info("Sent booking reminder email to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send booking reminder email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationEmail(
            String toEmail,
            String customerName,
            String stadiumName,
            Integer bookingId,
            LocalDate reservationDate,
            TimeSlot timeSlot,
            BigDecimal totalPrice
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Xác nhận đặt sân thành công — SportsBook");
            helper.setText(buildBookingConfirmationEmailBody(customerName, stadiumName, bookingId, reservationDate, timeSlot, totalPrice), true);
            mailSender.send(message);
            log.info("Sent booking confirmation email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send booking confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendBookingCancellationEmail(
            String toEmail,
            String customerName,
            String stadiumName,
            Integer bookingId,
            String reason,
            String cancelledBy
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Thông báo hủy đơn đặt sân — SportsBook");
            helper.setText(buildBookingCancellationEmailBody(customerName, stadiumName, bookingId, reason, cancelledBy), true);
            mailSender.send(message);
            log.info("Sent booking cancellation email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send booking cancellation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendRefundEmail(
            String toEmail,
            String customerName,
            String stadiumName,
            Integer bookingId,
            BigDecimal refundAmount,
            int refundPercentage,
            BigDecimal originalAmount
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Thông báo hoàn tiền — SportsBook");
            helper.setText(buildRefundEmailBody(customerName, stadiumName, bookingId, refundAmount, refundPercentage, originalAmount), true);
            mailSender.send(message);
            log.info("Sent refund email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send refund email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendComplaintCreatedEmail(
            String toEmail,
            String ownerName,
            String stadiumName,
            Integer complaintId,
            String customerName,
            String subject
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Khách hàng khiếu nại mới — SportsBook");
            helper.setText(buildComplaintCreatedEmailBody(ownerName, stadiumName, complaintId, customerName, subject), true);
            mailSender.send(message);
            log.info("Sent complaint created email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send complaint created email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendComplaintResolvedEmail(
            String toEmail,
            String customerName,
            Integer complaintId,
            String resolution
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Phản hồi xử lý khiếu nại — SportsBook");
            helper.setText(buildComplaintResolvedEmailBody(customerName, complaintId, resolution), true);
            mailSender.send(message);
            log.info("Sent complaint resolved email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send complaint resolved email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendReviewRequestEmail(
            String toEmail,
            String customerName,
            String stadiumName,
            Integer bookingId,
            LocalDate reservationDate
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Đánh giá trải nghiệm sân " + stadiumName + " — SportsBook");
            helper.setText(buildReviewRequestEmailBody(customerName, stadiumName, bookingId, reservationDate), true);
            mailSender.send(message);
            log.info("Sent review request email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send review request email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendCustomerRegistrationSuccessEmail(String toEmail, String customerName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Đăng ký tài khoản thành công — Chào mừng tới SportsBook");
            helper.setText(buildCustomerRegistrationSuccessEmailBody(customerName), true);
            mailSender.send(message);
            log.info("Sent customer registration success email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send customer registration success email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOwnerRegistrationSuccessEmail(String toEmail, String ownerName, String businessName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Hồ sơ đối tác đã được phê duyệt — SportsBook");
            helper.setText(buildOwnerRegistrationSuccessEmailBody(ownerName, businessName), true);
            mailSender.send(message);
            log.info("Sent owner registration success email to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send owner registration success email to {}: {}", toEmail, e.getMessage());
        }
    }









    @Async
    public void sendPaymentFailedEmail(String toEmail, String customerName, Integer bookingId, String stadiumName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Thanh toán thất bại — Đơn đặt sân #" + bookingId);
            String htmlBody = buildPaymentFailedEmailBody(customerName, bookingId, stadiumName);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent payment failed email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send payment failed email to {}", toEmail, e);
        }
    }

    @Async
    public void sendOwnerNewBookingEmail(String toEmail, String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String customerName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Đơn đặt sân mới — " + stadiumName);
            String htmlBody = buildOwnerNewBookingEmailBody(ownerName, bookingId, stadiumName, date, time, customerName);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent owner new booking email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send owner new booking email to {}", toEmail, e);
        }
    }

    @Async
    public void sendOwnerBookingCancelledEmail(String toEmail, String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Khách hủy đơn đặt sân — " + stadiumName);
            String htmlBody = buildOwnerBookingCancelledEmailBody(ownerName, bookingId, stadiumName, date, time, reason);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent owner booking cancelled email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send owner booking cancelled email to {}", toEmail, e);
        }
    }

    @Async
    public void sendStadiumApprovalResultEmail(String toEmail, String ownerName, String stadiumName, boolean isApproved, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Kết quả duyệt sân " + stadiumName + " — SportsBook");
            String htmlBody = buildStadiumApprovalResultEmailBody(ownerName, stadiumName, isApproved, reason);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent stadium approval result email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send stadium approval result email to {}", toEmail, e);
        }
    }





    private String safe(String value) {
        return value != null ? org.springframework.web.util.HtmlUtils.htmlEscape(value) : "";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(amount) + " VNĐ";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    private String formatTime(java.time.LocalTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
    }

    private String formatTimeRange(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return "";
        }
        return formatTime(timeSlot.getStartTime()) + " - " + formatTime(timeSlot.getEndTime());
    }

    private String buildEmailLayout(String title, String subtitle, String badgeText, String badgeColor, String bodyContent) {
        String badgeHtml = (badgeText != null && !badgeText.isBlank()) 
            ? "<div style=\"margin: 16px 0;text-align:center;\"><span style=\"background-color:" + badgeColor + ";color:white;padding:6px 16px;border-radius:20px;font-size:14px;font-weight:bold;\">" + safe(badgeText) + "</span></div>"
            : "";
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,Helvetica,sans-serif;color:#1f2937;background-color:#f3f6fb;margin:0;padding:20px;line-height:1.6;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                        <tr>
                            <td align="center">
                                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:16px;box-shadow:0 4px 6px rgba(0,0,0,0.05);overflow:hidden;margin:0 auto;">
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 20px;text-align:center;">
                                            <h1 style="color:#ffffff;margin:0;font-size:28px;">SportsBook</h1>
                                            <p style="color:#e0e7ff;margin:8px 0 0 0;font-size:16px;">Nền tảng đặt sân thể thao</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:28px 32px;">
                                            <h2 style="margin-top:0;color:#1f2937;font-size:22px;text-align:center;">%s</h2>
                                            %s
                                            <p style="text-align:center;color:#4b5563;margin-bottom:24px;">%s</p>
                                            %s
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                                            <p style="margin:0;color:#6b7280;font-size:13px;">© SportsBook. Email này được gửi tự động, vui lòng không trả lời trực tiếp.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(safe(title), badgeHtml, safe(subtitle), bodyContent);
    }

    private String detailRow(String label, String value) {
        return "<tr>" +
               "<td style=\"padding:12px 16px;border-bottom:1px solid #e5e7eb;color:#6b7280;font-weight:bold;font-size:14px;\" width=\"40%\">" + safe(label) + "</td>" +
               "<td style=\"padding:12px 16px;border-bottom:1px solid #e5e7eb;color:#1f2937;font-size:15px;font-weight:500;text-align:right;\" width=\"60%\">" + safe(value) + "</td>" +
               "</tr>";
    }

    private String getDetailCardStart() {
        return "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#f8fafc;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;margin-bottom:24px;\">";
    }

    private String getDetailCardEnd() {
        return "</table>";
    }

    private String getButtonHtml(String text) {
        return "<div style=\"text-align:center;margin-top:16px;\">" +
               "<span style=\"display:inline-block;background-color:#1a73e8;color:white;padding:12px 24px;border-radius:8px;font-weight:bold;font-size:15px;\">" + safe(text) + "</span>" +
               "</div>";
    }

    private String buildCustomerRegistrationSuccessEmailBody(String customerName) {
        String body = "<p>Chào mừng bạn đã đến với SportsBook! Tài khoản của bạn đã được tạo thành công.</p>" +
                      getButtonHtml("Bắt đầu đặt sân ngay");
        return buildEmailLayout("Đăng ký thành công", "Xin chào " + customerName, "Thành viên mới", "#10b981", body);
    }

    private String buildOwnerRegistrationSuccessEmailBody(String ownerName, String businessName) {
        String body = "<p>Cảm ơn bạn đã đăng ký trở thành đối tác của SportsBook. Hồ sơ sân <strong>" + safe(businessName) + "</strong> của bạn đã được ghi nhận.</p>" +
                      "<p>Chúng tôi sẽ liên hệ và xét duyệt trong thời gian sớm nhất.</p>";
        return buildEmailLayout("Đăng ký đối tác", "Xin chào " + ownerName, "Thành công", "#10b981", body);
    }

    private String buildOtpEmailBody(String otpCode, int expiryMinutes) {
        String body = "<p>Bạn đã yêu cầu một mã OTP để xác thực.</p>" +
                      "<div style=\"text-align:center;margin:24px 0;\"><span style=\"background-color:#f3f4f6;padding:16px 32px;font-size:32px;font-weight:bold;letter-spacing:8px;border-radius:12px;color:#1f2937;\">" + safe(otpCode) + "</span></div>" +
                      "<p>Mã này có hiệu lực trong vòng " + expiryMinutes + " phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>";
        return buildEmailLayout("Xác thực OTP", "Mã xác thực của bạn", "Bảo mật", "#3b82f6", body);
    }

    private String buildBookingConfirmationEmailBody(String customerName, String stadiumName, Integer bookingId, LocalDate reservationDate, TimeSlot timeSlot, BigDecimal totalPrice) {
        String body = "<p>Cảm ơn bạn đã đặt sân tại SportsBook. Đơn đặt sân của bạn đã được xác nhận thành công.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Tên sân", stadiumName) +
                      detailRow("Ngày đặt", formatDate(reservationDate)) +
                      detailRow("Khung giờ", formatTimeRange(timeSlot)) +
                      detailRow("Tổng tiền", formatCurrency(totalPrice)) +
                      getDetailCardEnd() +
                      "<p style=\"color:#d97706;font-style:italic;text-align:center;\">Vui lòng đến đúng giờ để có trải nghiệm tốt nhất!</p>" +
                      getButtonHtml("Xem chi tiết đơn");
        return buildEmailLayout("Xác nhận đặt sân", "Xin chào " + customerName, "Đã xác nhận", "#10b981", body);
    }

    private String buildBookingCancellationEmailBody(String customerName, String stadiumName, Integer bookingId, String reason, String cancelledBy) {
        String body = "<p>Đơn đặt sân của bạn đã bị hủy.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Tên sân", stadiumName) +
                      detailRow("Người hủy", cancelledBy) +
                      detailRow("Lý do", reason != null && !reason.isBlank() ? reason : "Không có lý do") +
                      getDetailCardEnd() +
                      "<p>Nếu bạn có thắc mắc, vui lòng liên hệ bộ phận hỗ trợ của chúng tôi.</p>" +
                      getButtonHtml("Liên hệ hỗ trợ");
        return buildEmailLayout("Hủy đơn đặt sân", "Xin chào " + customerName, "Đã hủy", "#ef4444", body);
    }

    private String buildRefundEmailBody(String customerName, String stadiumName, Integer bookingId, BigDecimal refundAmount, int refundPercentage, BigDecimal originalAmount) {
        String body = "<p>Chúng tôi đã tiến hành hoàn tiền cho đơn đặt sân của bạn.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Tên sân", stadiumName) +
                      detailRow("Tổng tiền ban đầu", formatCurrency(originalAmount)) +
                      detailRow("Tỷ lệ hoàn tiền", refundPercentage + "%") +
                      detailRow("Số tiền hoàn", formatCurrency(refundAmount)) +
                      getDetailCardEnd() +
                      "<p style=\"font-size:13px;color:#6b7280;\">Lưu ý: Thời gian tiền về tài khoản có thể phụ thuộc vào ngân hàng hoặc cổng thanh toán của bạn.</p>";
        return buildEmailLayout("Hoàn tiền thành công", "Xin chào " + customerName, "Đã hoàn tiền", "#8b5cf6", body);
    }

    private String buildComplaintCreatedEmailBody(String ownerName, String stadiumName, Integer complaintId, String customerName, String subject) {
        String body = "<p>Có một khiếu nại mới từ khách hàng đối với sân của bạn.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã khiếu nại", "#" + complaintId) +
                      detailRow("Tên sân", stadiumName) +
                      detailRow("Khách hàng", customerName) +
                      detailRow("Tiêu đề", subject) +
                      getDetailCardEnd() +
                      "<p>Bạn vui lòng kiểm tra và phản hồi khách hàng trong thời gian sớm nhất để đảm bảo chất lượng dịch vụ.</p>" +
                      getButtonHtml("Xem chi tiết khiếu nại");
        return buildEmailLayout("Khiếu nại mới", "Xin chào " + ownerName, "Khiếu nại mới", "#f97316", body);
    }

    private String buildComplaintResolvedEmailBody(String customerName, Integer complaintId, String resolution) {
        String body = "<p>Khiếu nại của bạn đã được ban quản trị và chủ sân xử lý.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã khiếu nại", "#" + complaintId) +
                      detailRow("Kết quả xử lý", resolution) +
                      getDetailCardEnd() +
                      "<p>Cảm ơn bạn đã phản hồi, ý kiến của bạn giúp chúng tôi cải thiện dịch vụ tốt hơn.</p>";
        return buildEmailLayout("Khiếu nại đã xử lý", "Xin chào " + customerName, "Đã xử lý", "#10b981", body);
    }

    private String buildReviewRequestEmailBody(String customerName, String stadiumName, Integer bookingId, LocalDate reservationDate) {
        String body = "<p>Cảm ơn bạn đã sử dụng dịch vụ đặt sân tại SportsBook.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Tên sân", stadiumName) +
                      detailRow("Ngày chơi", formatDate(reservationDate)) +
                      getDetailCardEnd() +
                      "<p>Bạn cảm thấy chất lượng dịch vụ như thế nào? Hãy dành vài phút đánh giá để giúp sân cải thiện tốt hơn nhé!</p>" +
                      getButtonHtml("Đánh giá sân");
        return buildEmailLayout("Mời đánh giá", "Xin chào " + customerName, "Mời đánh giá", "#3b82f6", body);
    }

    private String buildPaymentFailedEmailBody(String customerName, Integer bookingId, String stadiumName) {
        String body = "<p>Rất tiếc, giao dịch thanh toán cho đơn đặt sân của bạn đã thất bại hoặc bị hủy.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Tên sân", stadiumName) +
                      getDetailCardEnd() +
                      "<p>Đơn đặt sân hiện vẫn ở trạng thái chờ. Vui lòng thực hiện lại giao dịch sớm nhất để giữ chỗ.</p>" +
                      getButtonHtml("Thanh toán lại");
        return buildEmailLayout("Thanh toán thất bại", "Xin chào " + customerName, "Thất bại", "#ef4444", body);
    }

    private String buildOwnerNewBookingEmailBody(String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String customerName) {
        String body = "<p>Bạn vừa có một đơn đặt sân mới đã được xác nhận thanh toán.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Khách hàng", customerName) +
                      detailRow("Thời gian", formatTime(time) + " ngày " + formatDate(date)) +
                      getDetailCardEnd() +
                      "<p>Vui lòng kiểm tra chi tiết trong bảng điều khiển chủ sân để chuẩn bị đón khách.</p>";
        return buildEmailLayout("Đơn đặt sân mới", "Xin chào " + ownerName, "Đơn mới", "#3b82f6", body);
    }

    private String buildOwnerBookingCancelledEmailBody(String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String reason) {
        String body = "<p>Một đơn đặt sân vừa bị khách hàng hủy.</p>" +
                      getDetailCardStart() +
                      detailRow("Mã đặt sân", "#" + bookingId) +
                      detailRow("Thời gian", formatTime(time) + " ngày " + formatDate(date)) +
                      detailRow("Lý do", reason != null && !reason.isBlank() ? reason : "Không có lý do") +
                      getDetailCardEnd() +
                      "<p>Khung giờ này đã được giải phóng và có sẵn cho khách hàng khác đặt.</p>";
        return buildEmailLayout("Khách hủy đơn", "Xin chào " + ownerName, "Đã hủy", "#ef4444", body);
    }

    private String buildStadiumApprovalResultEmailBody(String ownerName, String stadiumName, boolean isApproved, String reason) {
        String badge = isApproved ? "Được duyệt" : "Bị từ chối";
        String color = isApproved ? "#10b981" : "#ef4444";
        String statusText = isApproved ? "đã được ĐƯỢC DUYỆT" : "đã BỊ TỪ CHỐI";
        String body = "<p>Hồ sơ đăng ký sân <strong>" + safe(stadiumName) + "</strong> của bạn " + statusText + " bởi quản trị viên.</p>";
        if (!isApproved) {
            body += getDetailCardStart() + detailRow("Lý do từ chối", reason != null ? reason : "") + getDetailCardEnd();
            body += "<p>Vui lòng đăng nhập vào bảng điều khiển, chỉnh sửa thông tin theo yêu cầu và gửi lại để được xét duyệt.</p>";
        } else {
            body += "<p>Sân của bạn hiện đã hiển thị trên hệ thống và sẵn sàng nhận khách đặt.</p>";
        }
        return buildEmailLayout("Kết quả duyệt sân", "Xin chào " + ownerName, badge, color, body);
    }
}
