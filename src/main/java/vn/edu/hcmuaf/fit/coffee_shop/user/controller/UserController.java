package vn.edu.hcmuaf.fit.coffee_shop.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtTokenUtil;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.UserRequest;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.UserResponse;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.VerificationToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.EmailService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.RefreshTokenService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.UserService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.VerificationTokenService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final VerificationTokenService verificationTokenService;
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
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        boolean verified = verificationTokenService.verifyToken(token);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xác thực email thành công! Bây giờ bạn có thể đăng nhập."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Link xác thực không hợp lệ hoặc đã hết hạn!"
            ));
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

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "fullName", user.getFullName(),
            "role", user.getRole().name(),
            "enabled", user.getEnabled(),
            "createdAt", user.getCreatedAt()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        return ResponseEntity.ok(Map.of(
            "message", "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi."
        ));
    }
}