package vn.edu.hcmuaf.fit.coffee_shop.user.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private String role;
    private Boolean enabled;
    private Boolean locked;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String message;
}