package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtTokenUtil;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;

    // Regex patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    @Transactional
    public UserResponse registerUser(UserRequest request) {
        try {
            // validate email format
            if (!isValidEmail(request.getEmail())) {
                return UserResponse.builder()
                    .message("Email không hợp lệ! Vui lòng nhập đúng định dạng email.")
                    .build();
            }

            // validate email domain
            if (!isValidEmailDomain(request.getEmail())) {
                return UserResponse.builder()
                    .message("Domain email không tồn tại hoặc không hợp lệ!")
                    .build();
            }

            // check email already exists
            Optional<User> existing = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
            if (existing.isPresent()) {
                User existingUser = existing.get();

                // check if account wasn't verified
                if (!existingUser.getEnabled()) {
                    return UserResponse.builder()
                        .message("Email đã được đăng ký nhưng chưa xác thực. Vui lòng kiểm tra email hoặc yêu cầu gửi lại.")
                        .build();
                }

                return UserResponse.builder()
                    .message("Email đã được sử dụng!")
                    .build();
            }

            // validate password strength
            if (!isStrongPassword(request.getPassword())) {
                return UserResponse.builder()
                    .message("Mật khẩU phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@#$%^&+=!)")
                    .build();
            }

            // validate full name
            if (request.getFullName() == null || request.getFullName().trim().length() < 2) {
                return UserResponse.builder()
                    .message("Họ tên phải có ít nhất 2 ký tự!")
                    .build();
            }

            // encoding password
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // save new user (unverified)
            User newUser = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(encodedPassword)
                .role(Role.USER)
                .enabled(false)
                .locked(false)
                .failedLoginAttempts(0)
                .build();

            userRepository.save(newUser);
            log.info("Đã tạo user mới: {}", newUser.getEmail());

            // create verification token
            VerificationToken token = verificationTokenService.createVerificationToken(newUser);

            // send verification email
            try {
                emailService.sendVerificationEmail(
                    newUser.getEmail(), newUser.getFullName(), token.getToken()
                );

                return UserResponse.builder()
                    .id(newUser.getId())
                    .fullName(newUser.getFullName())
                    .email(newUser.getEmail())
                    .message("Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản. Email có thể nằm trong thư mục Spam.")
                    .build();
            } catch (Exception e) {
                log.error("Lỗi gửi email: {}", e.getMessage());
                return UserResponse.builder()
                    .id(newUser.getId())
                    .fullName(newUser.getFullName())
                    .email(newUser.getEmail())
                    .message("Đăng ký thành công nhưng không thể gửi email xác thực. Vui lòng liên hệ admin.")
                    .build();
            }
        } catch (Exception e) {
            log.error("Lỗi đăng ký: {}", e.getMessage(), e);
            return UserResponse.builder()
                .message("Có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!")
                .build();
        }
    }

    @Transactional
    public UserResponse login(String email, String rawPassword) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

            if (userOpt.isEmpty()) {
                return UserResponse.builder()
                    .message("Email không đúng!")
                    .build();
            }

            User user = userOpt.get();

            // check if account is locked
            if (!user.isAccountNonLocked()) {
                return UserResponse.builder()
                    .message("Tài khoản đã bị khoá do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + LOCKOUT_DURATION_MINUTES + " phút.")
                    .build();
            }

            // check if account was verified
            if (!user.getEnabled()) {
                return UserResponse.builder()
                    .message("Tài khoản chưa được xác thực. Vui lòng kiểm tra email để kích hoạt tài khoản.")
                    .build();
            }

            // compare passwords
            boolean match = passwordEncoder.matches(rawPassword, user.getPassword());

            if (!match) {
                // increase failed login attempts
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

                // lock account if the allowed number of times is exceeded
                if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                    user.setLocked(true);
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                    userRepository.save(user);

                    log.warn("Tài khoản {} đã bị khoá do đăng nhập sai quá {} lần", email, MAX_LOGIN_ATTEMPTS);

                    return UserResponse.builder()
                        .message("Tài khoản đã bị khoá do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + LOCKOUT_DURATION_MINUTES + " phút.")
                        .build();
                }

                userRepository.save(user);

                int remainingAttempts = MAX_LOGIN_ATTEMPTS - user.getFailedLoginAttempts();
                return UserResponse.builder()
                    .message("Mật khẩu không đúng! Còn " + remainingAttempts + " lần thử.")
                    .build();
            }

            // login successfully
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // create JWT token
            String accessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("User {} đăng nhập thành công", email);

            return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("Đăng nhập thành công!")
                .build();
        } catch (Exception e) {
            log.error("Lỗi đăng nhập: {}", e.getMessage(), e);
            return UserResponse.builder()
                .message("Có lỗi xảy ra trong quá trình đăng nhập!")
                .build();
        }
    }

    @Transactional
    public boolean resetPassword(User user, String newPassword) {
        try {
            if (!isStrongPassword(newPassword)) {
                log.error("Mật khẩu mới không đủ mạnh cho user: {}", user.getEmail());
                return false;
            }

            String encodedPassword = passwordEncoder.encode(newPassword);
            
            user.setPassword(encodedPassword);
            
            // Reset failed login attempts
            user.setFailedLoginAttempts(0);
            user.setLocked(false);
            user.setLockedUntil(null);
            
            userRepository.save(user);
            
            log.info("Đã reset password thành công cho user: {}", user.getEmail());
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi reset password cho user {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidEmailDomain(String email) {
        try {
            String domain = email.substring(email.indexOf("@") + 1);

            String[] trustedDomains = {
                "gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
                "icloud.com", "protonmail.com", "zoho.com", "aol.com"
            };

            for (String trustedDomain : trustedDomains) {
                if (domain.equalsIgnoreCase(trustedDomain)) {
                    return true;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}