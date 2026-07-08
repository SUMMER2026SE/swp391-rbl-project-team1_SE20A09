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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.web.util.HtmlUtils;

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

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    /**
     * Gửi email OTP xác thực tài khoản sau đăng ký.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
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
        log.info("Preparing to send account status notification email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Thông báo trạng thái tài khoản đối tác — SportsBook");

            String status = isEnabled ? "MỞ KHÓA" : "BỊ KHÓA";
            String textContent = "Xin chào " + businessName + ",\n\n" +
                    "Tài khoản đối tác của bạn trên hệ thống SportsBook vừa được " + status + ".\n";
            
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
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("⏰ Nhắc lịch chơi thể thao - " + stadiumName);
            message.setText(
                    "Xin chào,\n\n" +
                    "Bạn có lịch đặt sân sắp tới:\n" +
                    "  🏟️  Sân:  " + stadiumName + "\n" +
                    "  📅  Ngày: " + reservationDate + "\n" +
                    "  🕐  Giờ:  " + timeRange + "\n\n" +
                    "Vui lòng có mặt đúng giờ. Nếu cần hỗ trợ, liên hệ với chúng tôi qua ứng dụng.\n\n" +
                    "Chúc bạn có buổi chơi thể thao vui vẻ!\n" +
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

    private String buildCustomerRegistrationSuccessEmailBody(String customerName) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Chúc mừng bạn đã đăng ký và xác thực tài khoản thành công trên <strong>SportsBook</strong>!</p>
                        <p>Giờ đây bạn có thể:</p>
                        <ul>
                            <li>Tìm kiếm và đặt sân thể thao nhanh chóng</li>
                            <li>Theo dõi và quản lý lịch đặt sân</li>
                            <li>Thanh toán trực tuyến dễ dàng</li>
                        </ul>
                        <p>Đăng nhập ngay để bắt đầu trải nghiệm.</p>
                        <p>Chúc bạn có những giờ phút tập luyện thể thao tuyệt vời!</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName));
    }

    private String buildOwnerRegistrationSuccessEmailBody(String ownerName, String businessName) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Hồ sơ đối tác cho doanh nghiệp <strong>%s</strong> của bạn đã được Admin phê duyệt thành công!</p>
                        <p>Tài khoản của bạn hiện đã được cấp quyền Chủ Sân (Owner). Bạn có thể:</p>
                        <ul>
                            <li>Đăng tải và quản lý thông tin sân thể thao</li>
                            <li>Tạo lịch trống và quản lý đơn đặt sân từ khách hàng</li>
                            <li>Thống kê doanh thu</li>
                        </ul>
                        <p>Vui lòng đăng nhập vào hệ thống để bắt đầu kinh doanh.</p>
                        <p>Chúc bạn kinh doanh hồng phát cùng SportsBook!</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(businessName));
    }

    private String buildOtpEmailBody(String otpCode, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Xác thực tài khoản SportsBook</title>
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
                                <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">⚽ SportsBook</span>
                              </div>
                              <p style="color:rgba(255,255,255,0.85);margin:12px 0 0;font-size:14px;">Nền tảng đặt sân thể thao trực tuyến</p>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h2 style="margin:0 0 8px;color:#1e293b;font-size:22px;font-weight:600;">Xác thực tài khoản của bạn</h2>
                              <p style="margin:0 0 28px;color:#64748b;font-size:15px;line-height:1.6;">
                                Chào mừng bạn đến với SportsBook! Vui lòng sử dụng mã OTP bên dưới để hoàn tất đăng ký tài khoản.
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
                                  <li>Quay lại trang xác thực trên SportsBook</li>
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
                              <p style="margin:0;color:#94a3b8;font-size:12px;">© 2026 SportsBook. Tất cả quyền được bảo lưu.</p>
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

    private String buildBookingConfirmationEmailBody(String customerName, String stadiumName, Integer bookingId, LocalDate reservationDate, TimeSlot timeSlot, BigDecimal totalPrice) {
        String formattedDate = reservationDate != null ? reservationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String formattedTime = timeSlot != null ? (timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))) : "";
        String formattedPrice = totalPrice != null ? NumberFormat.getInstance(new Locale("vi", "VN")).format(totalPrice) + " VNĐ" : "";
        
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Bạn đã đặt sân thành công tại <strong>%s</strong>.</p>
                        <ul>
                            <li><strong>Mã đặt sân:</strong> %d</li>
                            <li><strong>Ngày:</strong> %s</li>
                            <li><strong>Khung giờ:</strong> %s</li>
                            <li><strong>Tổng tiền:</strong> %s</li>
                        </ul>
                        <p>Chúc bạn có buổi chơi thể thao vui vẻ!</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), HtmlUtils.htmlEscape(stadiumName), bookingId, formattedDate, formattedTime, formattedPrice);
    }

    private String buildBookingCancellationEmailBody(String customerName, String stadiumName, Integer bookingId, String reason, String cancelledBy) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Đơn đặt sân <strong>#%d</strong> của bạn tại <strong>%s</strong> đã bị hủy.</p>
                        <p><strong>Người hủy:</strong> %s</p>
                        <p><strong>Lý do hủy:</strong> %s</p>
                        <p>Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ bộ phận hỗ trợ.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), bookingId, HtmlUtils.htmlEscape(stadiumName), HtmlUtils.htmlEscape(cancelledBy), HtmlUtils.htmlEscape(reason));
    }

    private String buildRefundEmailBody(String customerName, String stadiumName, Integer bookingId, BigDecimal refundAmount, int refundPercentage, BigDecimal originalAmount) {
        String formattedRefund = refundAmount != null ? NumberFormat.getInstance(new Locale("vi", "VN")).format(refundAmount) + " VNĐ" : "";
        String formattedOriginal = originalAmount != null ? NumberFormat.getInstance(new Locale("vi", "VN")).format(originalAmount) + " VNĐ" : "";
        
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Đơn đặt sân <strong>#%d</strong> của bạn tại <strong>%s</strong> đã được xử lý hoàn tiền.</p>
                        <ul>
                            <li><strong>Tổng tiền ban đầu:</strong> %s</li>
                            <li><strong>Tỷ lệ hoàn:</strong> %d%%</li>
                            <li><strong>Số tiền hoàn lại:</strong> %s</li>
                        </ul>
                        <p>Tiền sẽ được hoàn vào tài khoản của bạn trong thời gian sớm nhất tùy thuộc vào ngân hàng.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), bookingId, HtmlUtils.htmlEscape(stadiumName), formattedOriginal, refundPercentage, formattedRefund);
    }

    private String buildComplaintCreatedEmailBody(String ownerName, String stadiumName, Integer complaintId, String customerName, String subject) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Sân <strong>%s</strong> của bạn vừa nhận được một khiếu nại từ khách hàng <strong>%s</strong>.</p>
                        <p><strong>Mã khiếu nại:</strong> %d</p>
                        <p><strong>Tiêu đề:</strong> %s</p>
                        <p>Vui lòng đăng nhập vào hệ thống để xem chi tiết và phản hồi khách hàng trong thời gian sớm nhất.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(stadiumName), HtmlUtils.htmlEscape(customerName), complaintId, HtmlUtils.htmlEscape(subject));
    }

    private String buildComplaintResolvedEmailBody(String customerName, Integer complaintId, String resolution) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Khiếu nại <strong>#%d</strong> của bạn đã được phản hồi xử lý.</p>
                        <p><strong>Nội dung phản hồi:</strong></p>
                        <blockquote style="border-left: 4px solid #ccc; margin: 1.5em 10px; padding: 0.5em 10px;">%s</blockquote>
                        <p>Cảm ơn bạn đã đóng góp ý kiến để chúng tôi cải thiện dịch vụ tốt hơn.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), complaintId, HtmlUtils.htmlEscape(resolution));
    }

    private String buildReviewRequestEmailBody(String customerName, String stadiumName, Integer bookingId, LocalDate reservationDate) {
        String formattedDate = reservationDate != null ? reservationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Cảm ơn bạn đã trải nghiệm tại <strong>%s</strong> vào ngày <strong>%s</strong> (Mã đặt sân: %d).</p>
                        <p>Bạn cảm thấy chất lượng dịch vụ như thế nào? Hãy dành vài phút để đánh giá và để lại nhận xét nhé.</p>
                        <p>Phản hồi của bạn rất quan trọng để giúp sân cải thiện tốt hơn.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), HtmlUtils.htmlEscape(stadiumName), formattedDate, bookingId);
    }
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

    private String buildPaymentFailedEmailBody(String customerName, Integer bookingId, String stadiumName) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#e53935,#b71c1c);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Rất tiếc, giao dịch thanh toán cho đơn đặt sân <strong>#%d</strong> tại <strong>%s</strong> đã thất bại hoặc bị hủy.</p>
                        <p>Đơn đặt sân của bạn hiện vẫn ở trạng thái chờ thanh toán. Vui lòng thực hiện lại giao dịch trong thời gian sớm nhất để giữ chỗ.</p>
                        <p>Nếu bạn gặp sự cố, xin vui lòng liên hệ bộ phận hỗ trợ.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(customerName), bookingId, HtmlUtils.htmlEscape(stadiumName));
    }

    private String buildOwnerNewBookingEmailBody(String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String customerName) {
        String formattedDate = date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String formattedTime = time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#1a73e8,#0d47a1);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Bạn vừa có một đơn đặt sân mới đã được xác nhận thanh toán.</p>
                        <ul>
                            <li><strong>Sân:</strong> %s</li>
                            <li><strong>Mã đơn:</strong> %d</li>
                            <li><strong>Khách hàng:</strong> %s</li>
                            <li><strong>Thời gian:</strong> %s ngày %s</li>
                        </ul>
                        <p>Bạn có thể kiểm tra chi tiết trong bảng điều khiển chủ sân.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(stadiumName), bookingId, HtmlUtils.htmlEscape(customerName), formattedTime, formattedDate);
    }

    private String buildOwnerBookingCancelledEmailBody(String ownerName, Integer bookingId, String stadiumName, LocalDate date, java.time.LocalTime time, String reason) {
        String formattedDate = date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String formattedTime = time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        String displayReason = (reason != null && !reason.isBlank()) ? reason : "Không có lý do";
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#ff9800,#f57c00);color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Một đơn đặt sân tại <strong>%s</strong> vừa bị khách hàng hủy.</p>
                        <ul>
                            <li><strong>Mã đơn:</strong> %d</li>
                            <li><strong>Thời gian bị hủy:</strong> %s ngày %s</li>
                            <li><strong>Lý do:</strong> %s</li>
                        </ul>
                        <p>Khung giờ này đã được giải phóng và có sẵn cho khách hàng khác đặt.</p>
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(stadiumName), bookingId, formattedTime, formattedDate, HtmlUtils.htmlEscape(displayReason));
    }

    private String buildStadiumApprovalResultEmailBody(String ownerName, String stadiumName, boolean isApproved, String reason) {
        String statusText = isApproved ? "được ĐƯỢC DUYỆT" : "bị TỪ CHỐI";
        String color = isApproved ? "linear-gradient(135deg,#4caf50,#2e7d32)" : "linear-gradient(135deg,#e53935,#b71c1c)";
        String reasonHtml = (!isApproved && reason != null && !reason.isBlank()) 
                ? "<p><strong>Lý do từ chối:</strong> " + HtmlUtils.htmlEscape(reason) + "</p>" 
                : "";
        String nextSteps = isApproved 
                ? "<p>Sân của bạn hiện đã hiển thị trên hệ thống và sẵn sàng nhận khách đặt.</p>"
                : "<p>Vui lòng đăng nhập vào bảng điều khiển, chỉnh sửa thông tin sân theo yêu cầu và gửi lại để được xét duyệt.</p>";
                
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8" /></head>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <div style="background:%s;color:white;padding:20px;text-align:center;">
                        <h2>SportsBook</h2>
                    </div>
                    <div style="padding:20px;">
                        <p>Xin chào %s,</p>
                        <p>Hồ sơ đăng ký sân <strong>%s</strong> của bạn vừa %s bởi quản trị viên.</p>
                        %s
                        %s
                    </div>
                    <div style="background:#f4f4f4;padding:10px;text-align:center;font-size:12px;">
                        © SportsBook
                    </div>
                </body>
                </html>
                """.formatted(color, HtmlUtils.htmlEscape(ownerName), HtmlUtils.htmlEscape(stadiumName), statusText, reasonHtml, nextSteps);
    }
}
