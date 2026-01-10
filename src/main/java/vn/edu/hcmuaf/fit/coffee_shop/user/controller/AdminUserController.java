package vn.edu.hcmuaf.fit.coffee_shop.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.Role;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.AdminUserService;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AdminUserResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<AdminUserResponse>> searchUsers(
            @RequestParam String keyword) {
        List<AdminUserResponse> users = adminUserService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> getUserDetails(
            @PathVariable Long userId) {
        AdminUserResponse user = adminUserService.getUserDetails(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<AdminUserResponse>> getUsersByRole(
            @PathVariable Role role) {
        List<AdminUserResponse> users = adminUserService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/locked")
    public ResponseEntity<List<AdminUserResponse>> getLockedUsers() {
        List<AdminUserResponse> users = adminUserService.getLockedUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/unverified")
    public ResponseEntity<List<AdminUserResponse>> getUnverifiedUsers() {
        List<AdminUserResponse> users = adminUserService.getUnverifiedUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        try {
            AdminUserResponse response = adminUserService.createUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        try {
            AdminUserResponse response = adminUserService.updateUser(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        try {
            String roleStr = body.get("role");
            Role role = Role.valueOf(roleStr);
            
            AdminUserResponse response = adminUserService.changeUserRole(userId, role);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<?> toggleLockUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        try {
            Boolean locked = body.get("locked");
            AdminUserResponse response = adminUserService.toggleLockUser(userId, locked);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/verify")
    public ResponseEntity<?> verifyUserEmail(
            @PathVariable Long userId) {
        try {
            AdminUserResponse response = adminUserService.verifyUserEmail(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.get("newPassword");
            adminUserService.resetUserPassword(userId, newPassword);
            return ResponseEntity.ok(Map.of("message", "Đã reset mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            adminUserService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa user thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        UserStatisticsResponse stats = adminUserService.getUserStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{userId}/login-history")
    public ResponseEntity<?> getUserLoginHistory(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportUsers(@RequestParam(defaultValue = "excel") String format) {
        return ResponseEntity.ok(Map.of("message", "Export feature coming soon"));
    }
}