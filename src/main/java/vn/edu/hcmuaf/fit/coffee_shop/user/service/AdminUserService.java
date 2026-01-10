package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.coffee_shop.user.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Lấy tất cả user với phân trang
     */
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToResponse);
    }

    /**
     * Tìm kiếm user theo từ khóa
     */
    public List<AdminUserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.searchByKeyword(keyword.trim().toLowerCase());
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết user
     */
    public AdminUserResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return convertToResponse(user);
    }

    /**
     * Lấy user theo role
     */
    public List<AdminUserResponse> getUsersByRole(Role role) {
        List<User> users = userRepository.findByRole(role);
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy user đã bị khóa
     */
    public List<AdminUserResponse> getLockedUsers() {
        List<User> users = userRepository.findByLocked(true);
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy user chưa xác thực
     */
    public List<AdminUserResponse> getUnverifiedUsers() {
        List<User> users = userRepository.findByEnabled(false);
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo user mới
     */
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        // Validate email
        if (userRepository.findByEmail(request.getEmail().toLowerCase().trim()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Validate role
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Role không hợp lệ. Chỉ chấp nhận USER hoặc ADMIN");
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Create user
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(encodedPassword)
                .role(role)
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .locked(false)
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin created new user: {}", savedUser.getEmail());

        // Send welcome email
        if (Boolean.TRUE.equals(request.getSendWelcomeEmail())) {
            try {
                // TODO: Create welcome email template
                log.info("Sending welcome email to: {}", savedUser.getEmail());
            } catch (Exception e) {
                log.error("Failed to send welcome email: {}", e.getMessage());
            }
        }

        return convertToResponse(savedUser);
    }

    /**
     * Cập nhật user
     */
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Update fields
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email đã tồn tại");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }

        if (request.getRole() != null) {
            try {
                Role role = Role.valueOf(request.getRole().toUpperCase());
                user.setRole(role);
            } catch (Exception e) {
                throw new RuntimeException("Role không hợp lệ");
            }
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getLocked() != null) {
            user.setLocked(request.getLocked());
            if (!request.getLocked()) {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Admin updated user: {}", updatedUser.getEmail());

        return convertToResponse(updatedUser);
    }

    /**
     * Thay đổi role
     */
    @Transactional
    public AdminUserResponse changeUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setRole(role);
        User updatedUser = userRepository.save(user);
        
        log.info("Admin changed user {} role to: {}", user.getEmail(), role);

        return convertToResponse(updatedUser);
    }

    /**
     * Khóa/Mở khóa user
     */
    @Transactional
    public AdminUserResponse toggleLockUser(Long userId, Boolean locked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setLocked(locked);
        
        if (!locked) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        } else {
            user.setLockedUntil(LocalDateTime.now().plusYears(10)); // Lock indefinitely
        }

        User updatedUser = userRepository.save(user);
        
        log.info("Admin {} user: {}", locked ? "locked" : "unlocked", user.getEmail());

        return convertToResponse(updatedUser);
    }

    /**
     * Xác thực email cho user
     */
    @Transactional
    public AdminUserResponse verifyUserEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setEnabled(true);
        User updatedUser = userRepository.save(user);
        
        log.info("Admin verified email for user: {}", user.getEmail());

        return convertToResponse(updatedUser);
    }

    /**
     * Reset mật khẩu
     */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setLockedUntil(null);

        userRepository.save(user);
        
        // Delete refresh tokens
        refreshTokenService.deleteByUser(user);
        
        log.info("Admin reset password for user: {}", user.getEmail());
    }

    /**
     * Xóa user (soft delete - lock account)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Không cho phép xóa ADMIN cuối cùng
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể xóa ADMIN cuối cùng trong hệ thống");
            }
        }

        // Soft delete: lock account permanently
        user.setLocked(true);
        user.setEnabled(false);
        user.setLockedUntil(LocalDateTime.now().plusYears(100));
        
        userRepository.save(user);
        
        log.info("Admin deleted user: {}", user.getEmail());
    }

    /**
     * Thống kê user
     */
    public UserStatisticsResponse getUserStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null 
            ? startDate.atStartOfDay() 
            : LocalDateTime.now().minusMonths(1);

        LocalDateTime end = endDate != null 
            ? endDate.atTime(23, 59, 59) 
            : LocalDateTime.now();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabled(true);
        long lockedUsers = userRepository.countByLocked(true);
        long unverifiedUsers = userRepository.countByEnabled(false);
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long regularUsers = userRepository.countByRole(Role.USER);

        List<User> newUsers = userRepository.findByCreatedAtBetween(start, end);
        long newUsersCount = newUsers.size();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<User> newUsersToday = userRepository.findByCreatedAtBetween(todayStart, LocalDateTime.now());
        long newUsersTodayCount = newUsersToday.size();

        double activeUserPercentage = totalUsers > 0 
            ? (activeUsers * 100.0 / totalUsers) 
            : 0;

        return UserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .unverifiedUsers(unverifiedUsers)
                .adminUsers(adminUsers)
                .regularUsers(regularUsers)
                .newUsersThisMonth(newUsersCount)
                .newUsersToday(newUsersTodayCount)
                .activeUserPercentage(Math.round(activeUserPercentage * 100.0) / 100.0)
                .startDate(start)
                .endDate(end)
                .build();
    }

    // Helper
    private AdminUserResponse convertToResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .locked(user.getLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}