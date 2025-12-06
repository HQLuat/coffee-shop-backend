package vn.edu.hcmuaf.fit.coffee_shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtAuthenticationFilter;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ============ PUBLIC ENDPOINTS - ĐẶT TRƯỚC ============
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/users/refresh").permitAll()
                        .requestMatchers("/api/orders/zalopay/callback").permitAll()

                        // ============ ADMIN ONLY - ĐẶT TRƯỚC /api/orders/** ============
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ⚠️ QUAN TRỌNG: Các endpoint cụ thể phải đặt TRƯỚC endpoint chung /api/orders/**
                        // Refund endpoints - ADMIN only
                        .requestMatchers("/api/orders/*/zalopay/refund").hasRole("ADMIN")
                        .requestMatchers("/api/orders/zalopay/refund/*").hasRole("ADMIN")

                        // ============ USER & ADMIN ENDPOINTS ============
                        .requestMatchers("/api/users/me", "/api/users/hello", "/api/users/logout").hasAnyRole("USER", "ADMIN")

                        // ⚠️ Endpoint chung phải đặt SAU các endpoint cụ thể
                        .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")

                        // ============ DEFAULT ============
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}