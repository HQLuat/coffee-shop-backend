package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtTokenUtil;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;

    public UserResponse registerUser(UserRequest request) {

        // check if email exists
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            return UserResponse.builder()
                    .message("Email đã được sử dụng!")
                    .build();
        }

        // encoding password
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // save new user
        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(encodedPassword)
                .role(Role.USER)
                .build();
        userRepository.save(newUser);

        // register successfully
        return UserResponse.builder()
                .id(newUser.getId())
                .fullName(newUser.getFullName())
                .email(newUser.getEmail())
                .message("Đăng ký thành công")
                .build();
    }

    public UserResponse login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return UserResponse.builder()
                    .message("Email không tồn tại!")
                    .build();
        }

        User user = userOpt.get();

        // compare password
        boolean match = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!match) {
            return UserResponse.builder()
                    .message("Mật khẩu không đúng!")
                    .build();
        }

        // create JWT token
        String accessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // login successfully
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("Đăng nhập thành công!")
                .build();
    }
}