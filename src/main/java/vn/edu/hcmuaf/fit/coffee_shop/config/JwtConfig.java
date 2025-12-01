package vn.edu.hcmuaf.fit.coffee_shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Configuration
@Getter
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void validateConfig() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable is not set. Please set it before starting the application.");
        }
        
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 characters long for security reasons.");
        }
    }
}