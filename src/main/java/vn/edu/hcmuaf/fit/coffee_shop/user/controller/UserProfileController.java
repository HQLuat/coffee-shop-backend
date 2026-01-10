package vn.edu.hcmuaf.fit.coffee_shop.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.UserService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            UserProfileResponse response = userService.getUserProfile(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping
    public ResponseEntity<?> updateUserProfile(
            @Valid @RequestBody UpdateUserRequest request, 
            Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            UserProfileResponse response = userService.updateUserProfile(email, request);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Có lỗi xảy ra khi cập nhật thông tin!"
            ));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body, 
            Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");
            String confirmPassword = body.get("confirmPassword");

            if (currentPassword == null || currentPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Vui lòng nhập mật khẩu hiện tại"
                ));
            }

            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Vui lòng nhập mật khẩu mới"
                ));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Mật khẩu xác nhận không khớp"
                ));
            }

            UpdateUserRequest request = new UpdateUserRequest();
            request.setCurrentPassword(currentPassword);
            request.setNewPassword(newPassword);
            request.setConfirmNewPassword(confirmPassword);

            userService.updateUserProfile(email, request);

            return ResponseEntity.ok(Map.of(
                "message", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Có lỗi xảy ra khi đổi mật khẩu!"
            ));
        }
    }
}