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
import vn.edu.hcmuaf.fit.coffee_shop.user.service.RefreshTokenService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@RequestBody UserRequest request) {
        UserResponse response = userService.registerUser(request);

        if (response.getId() == null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        return ResponseEntity.ok(Map.of("email", email, "message", "Access granted"));
    }
}