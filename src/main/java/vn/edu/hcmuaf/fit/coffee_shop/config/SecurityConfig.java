package vn.edu.hcmuaf.fit.coffee_shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/refresh").permitAll()
                        // Email verification
                        .requestMatchers(HttpMethod.GET, "/api/users/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/verify_success.html", "/verify_fail.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/resend-verification").permitAll()
                        // Password reset
                        .requestMatchers(HttpMethod.POST, "/api/users/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reset_password.html", "/reset_password_fail.html").permitAll()
                        // ZaloPay
                        .requestMatchers("/api/orders/zalopay/callback").permitAll()

                        // Admin endpoints - CHỈ TẠO REFUND CẦN ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/zalopay/refund").hasRole("ADMIN")  // ✅ Chỉ POST cần ADMIN

                        // User + Admin endpoints - QUERY THÌ AI CŨNG ĐƯỢC
                        .requestMatchers(HttpMethod.GET, "/api/orders/zalopay/refund/*").hasAnyRole("USER", "ADMIN")  // ✅ GET cho phép USER
                        .requestMatchers(HttpMethod.POST, "/api/users/logout").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")

                        // Tất cả request khác
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}