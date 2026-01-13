package vn.edu.hcmuaf.fit.coffee_shop.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    // ======== email verification ========
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private Integer failedLoginAttempts;

    @Column
    private LocalDateTime lockedUntil;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    public boolean isAccountNonLocked() {
        if (!locked) return true;
        if (lockedUntil == null) return false;

        // If the lock time has expired, it will automatically unlock
        if (LocalDateTime.now().isAfter(lockedUntil)) {
            locked = false;
            lockedUntil = null;
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }
}