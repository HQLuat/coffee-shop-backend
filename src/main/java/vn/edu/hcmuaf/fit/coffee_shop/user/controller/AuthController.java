package vn.edu.hcmuaf.fit.coffee_shop.user.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtTokenUtil;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.ForgotPasswordRequest;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.ResetPasswordRequest;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.UserRequest;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.UserResponse;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.PasswordResetToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.VerificationToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.EmailService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.PasswordResetService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.RefreshTokenService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.UserService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.VerificationTokenService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final VerificationTokenService verificationTokenService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@RequestBody UserRequest request) {
        UserResponse response = userService.registerUser(request);

        if (response.getId() == null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        boolean verified = verificationTokenService.verifyToken(token);

        if (verified) {
            response.sendRedirect("/verify_success.html");
        } else {
            response.sendRedirect("/verify_fail.html");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Email không được để trống!"
            ));
        }

        User user = userRepository.findByEmail(email.toLowerCase().trim())
            .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Map.of(
                "message", "Nếu email tồn tại trong hệ thống, email xác thực đã được gửi."
            ));
        }

        // check if account is verified
        if (user.getEnabled()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Tài khoản đã được xác thực rồi!"
            ));
        }

        try {
            VerificationToken token = verificationTokenService.regenerateToken(user);
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token.getToken());

            return ResponseEntity.ok(Map.of(
                "message", "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư đến và thư mục Spam."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Không thể gửi email. Vui lòng thử lại sau!"
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        UserResponse response = userService.login(email, password);

        if (response.getRefreshToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshTokenString = body.get("refreshToken");

        return refreshTokenService.findByToken(refreshTokenString)
            .map(token -> {
                refreshTokenService.deleteByUser(token.getUser());
                return ResponseEntity.ok(Map.of("message", "Dang xuat thanh cong!"));
            })
            .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("message", "Token khong hop le")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshTokenString = body.get("refreshToken");

        return refreshTokenService.findByToken(refreshTokenString)
            .map(token -> {
                if (!refreshTokenService.verifyExpiration(token)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Refresh token da het han"));
                }

                User user = token.getUser();
                String newAccessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

                return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", refreshTokenString
                ));
            })
            .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("message", "Refresh token khong hop le")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String email = request.getEmail().toLowerCase().trim();

            User user = userRepository.findByEmail(email).orElse(null);

            // check if account exists
            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "message", "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi đến email của bạn."
                ));
            }

            // check if account is enable
            if (!user.getEnabled()) {
                return ResponseEntity.ok(Map.of(
                    "message", "Tài khoản chưa được xác thực."
                ));
            }

            PasswordResetToken token = passwordResetService.createPasswordResetToken(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token.getToken());

            return ResponseEntity.ok(Map.of(
                "message", "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi đến email của bạn."
            ));
        } catch (Exception e) {
            System.err.println("Error in forgot password: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                "message", "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi đến email của bạn."
            ));
        }
    }

    @GetMapping("/reset-password")
    public void showResetPasswordPage(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        boolean isValid = passwordResetService.verifyToken(token);

        if (isValid) {
            response.sendRedirect("/reset_password.html?token=" + token);
        } else {
            response.sendRedirect("/reset_password_fail.html");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            // compare new password and confirmation password
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Mật khẩu xác nhận không khớp!"
                ));
            }

            // verify token
            PasswordResetToken token = passwordResetService.findByToken(request.getToken())
                .orElse(null);

            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Token không hợp lệ!"
                ));
            }

            if (token.isUsed()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Token đã được sử dụng!"
                ));
            }

            if (token.isExpired()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Token đã hết hạn! Vui lòng yêu cầu reset password lại."
                ));
            }

            // update password
            boolean success = userService.resetPassword(token.getUser(), request.getNewPassword());

            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Không thể đặt lại mật khẩu. Vui lòng thử lại!"
                ));
            }

            passwordResetService.markTokenAsUsed(token);

            refreshTokenService.deleteByUser(token.getUser());

            return ResponseEntity.ok(Map.of(
                "message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại."
            ));
        } catch (Exception e) {
            System.err.println("Error in reset password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Có lỗi xảy ra. Vui lòng thử lại!"
            ));
        }
    }
}