package vn.edu.hcmuaf.fit.coffee_shop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.Role;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Root Admin credentials from environment variables
    @Value("${root.admin.email:admin@coffeeshop.com}")
    private String rootAdminEmail;

    @Value("${root.admin.password:Admin@123456}")
    private String rootAdminPassword;

    @Value("${root.admin.fullname:Root Administrator}")
    private String rootAdminFullname;

    @Override
    public void run(String... args) throws Exception {
        createRootAdminIfNotExists();
    }

    private void createRootAdminIfNotExists() {
        try {
            // Check if root admin already exists
            Optional<User> existingAdmin = userRepository.findByEmail(rootAdminEmail);

            if (existingAdmin.isPresent()) {
                return;
            }

            // Create new root admin
            User rootAdmin = User.builder()
                    .fullName(rootAdminFullname)
                    .email(rootAdminEmail)
                    .password(passwordEncoder.encode(rootAdminPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .locked(false)
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(rootAdmin);
        } catch (Exception e) {
            log.error("Error creating Root Admin: {}", e.getMessage(), e);
        }
    }
}