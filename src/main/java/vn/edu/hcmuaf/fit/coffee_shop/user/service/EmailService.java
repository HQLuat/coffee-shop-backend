package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác thực tài khoản Coffee Shop");

            String verificationLink = baseUrl + "/api/users/verify?token=" + token;

            String htmlContent = buildVerificationEmailHtml(fullName, verificationLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email xác thực đến: {}", to);
        } catch (MessagingException e) {
            log.error("Lỗi gửi email xác thực: {}", e.getMessage());
            throw new RuntimeException("Không thể gửi email xác thực", e);
        }
    }

    // Login notification on new device
    public void sendLoginNotification(String to, String fullName, String ipAddress, String location) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Cảnh báo: Đăng nhập từ thiết bị mới");

            String htmlContent = buildLoginNotificationHtml(fullName, ipAddress, location);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email thông báo đăng nhập đến: {}", to);
        } catch (MessagingException e) {
            log.error("Lỗi gửI email thông báo: {}", e.getMessage());
        }
    }

    // Reset password
    public void sendPasswordResetEmail(String to, String fullName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt lại mật khẩu");

            String resetLink = baseUrl + "/api/users/reset-password?token=" + resetToken;

            String htmlContent = buildPasswordResetEmailHtml(fullName, resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email reset mật khẩu đến: {}", to);
        } catch (MessagingException e) {
            log.error("Lỗi gửi email reset mật khẩu: {}", e.getMessage());
            throw new RuntimeException("Không thể gửi email reset mật khẩu", e);
        }
    }

    // ===== HTML EMAIL TEMPLATES =====
    private String buildVerificationEmailHtml(String fullName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #6F4E37; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { 
                        display: inline-block; 
                        padding: 12px 30px; 
                        background: #6F4E37; 
                        color: white !important;
                        text-decoration: none;
                        border-radius: 5px; 
                        margin: 20px 0;
                        cursor: pointer;
                    }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>☕ Coffee Shop</h1>
                    </div>
                    <div class="content">
                        <h2>Xin chào %s! 👋</h2>
                        <p>Cảm ơn bạn đã đăng ký tài khoản Coffee Shop.</p>
                        <p>Để hoàn tất đăng ký, vui lòng nhấn vào nút bên dưới để xác thực email của bạn:</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Xác thực Email</a>
                        </div>
                        <p>Hoặc copy link sau vào trình duyệt:</p>
                        <p style="word-break: break-all; background: #fff; padding: 10px; border: 1px solid #ddd;">
                            %s
                        </p>
                        <p><strong>⚠️ Link này sẽ hết hạn sau 24 giờ.</strong></p>
                        <p>Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Coffee Shop. All rights reserved.</p>
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fullName, verificationLink, verificationLink);
    }
    
    private String buildLoginNotificationHtml(String fullName, String ipAddress, String location) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #dc3545; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .info-box { background: white; padding: 15px; border-left: 4px solid #dc3545; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 Cảnh báo bảo mật</h1>
                    </div>
                    <div class="content">
                        <h2>Xin chào %s,</h2>
                        <p>Chúng tôi phát hiện đăng nhập mới vào tài khoản của bạn:</p>
                        <div class="info-box">
                            <p><strong>📍 Địa chỉ IP:</strong> %s</p>
                            <p><strong>🌍 Vị trí:</strong> %s</p>
                            <p><strong>🕐 Thời gian:</strong> Vừa xong</p>
                        </div>
                        <p>Nếu đây không phải là bạn, vui lòng đổi mật khẩu ngay lập tức và liên hệ với chúng tôi.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fullName, ipAddress, location);
    }
    
    private String buildPasswordResetEmailHtml(String fullName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #6F4E37; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { 
                        display: inline-block; 
                        padding: 12px 30px; 
                        background: #6F4E37; 
                        color: white !important; 
                        text-decoration: none; 
                        border-radius: 5px; 
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔑 Đặt lại mật khẩu</h1>
                    </div>
                    <div class="content">
                        <h2>Xin chào %s,</h2>
                        <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Đặt lại mật khẩu</a>
                        </div>
                        <p><strong>⚠️ Link này sẽ hết hạn sau 1 giờ.</strong></p>
                        <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fullName, resetLink);
    }
}
