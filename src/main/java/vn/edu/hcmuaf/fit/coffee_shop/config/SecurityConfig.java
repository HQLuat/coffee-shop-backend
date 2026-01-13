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

                        // ===== PUBLIC ENDPOINTS =====
                        // Authentication endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/resend-verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        // Static pages
                        .requestMatchers(HttpMethod.GET, "/verify_success.html", "/verify_fail.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reset_password.html", "/reset_password_fail.html")
                        .permitAll()
                        // ZaloPay
                        .requestMatchers("/api/orders/zalopay/callback").permitAll()
                        // Voucher
                        .requestMatchers(HttpMethod.GET, "/api/vouchers/active").permitAll()
                        // Product
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // ===== ADMIN ONLY ENDPOINTS =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // User Management
                        .requestMatchers(HttpMethod.GET, "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/users/**").hasRole("ADMIN")
                        // Refund
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/zalopay/refund").hasRole("ADMIN") // ✅ Chỉ POST
                                                                                                           // cần ADMIN
                        // Voucher
                        .requestMatchers(HttpMethod.POST, "/api/vouchers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vouchers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vouchers/*").hasRole("ADMIN")
                        // product
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // THÊM 2 DÒNG NÀY CHO ADMIN REVIEWS
                        .requestMatchers(HttpMethod.GET, "/api/admin/reviews").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/reviews/*").hasRole("ADMIN")
                        // ===== USER + ADMIN ENDPOINTS =====
                        // Auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").hasAnyRole("USER", "ADMIN")
                        // Profile
                        .requestMatchers("/api/profile/**").hasAnyRole("USER", "ADMIN")
                        // Order
                        .requestMatchers(HttpMethod.GET, "/api/orders/zalopay/refund/*").hasAnyRole("USER", "ADMIN") // ✅
                                                                                                                     // GET
                                                                                                                     // cho
                                                                                                                     // phép
                                                                                                                     // USER
                        .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                        // Cart
                        .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")
                        // Voucher
                        .requestMatchers(HttpMethod.GET, "/api/vouchers/*").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/vouchers/apply").hasAnyRole("USER", "ADMIN")
                        // Product
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("USER", "ADMIN")
                        // Reviews - POST, PUT cho USER + ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/*").hasAnyRole("USER", "ADMIN")
                        // ===== DEFAULT =====
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}