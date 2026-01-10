package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.PasswordResetToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.PasswordResetTokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final PasswordResetTokenRepository tokenRepository;

    @Transactional
    public PasswordResetToken createPasswordResetToken(User user) {
        // delete old token
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        PasswordResetToken token = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .build();

        PasswordResetToken savedToken = tokenRepository.save(token);
        log.info("Đã tạo password reset token cho user: {}", user.getEmail());

        return savedToken;
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public boolean verifyToken(String tokenString) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenString);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Token không tồn tại: {}", tokenString);
            return false;
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isUsed()) {
            log.warn("Token đã được sử dụng: {}", tokenString);
            return false;
        }

        if (token.isExpired()) {
            log.warn("Token đã hết hạn: {}", tokenString);
            return false;
        }

        return true;
    }

    @Transactional
    public void markTokenAsUsed(PasswordResetToken token) {
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        log.info("Đã đánh dấu token đã sử dụng cho user: {}", token.getUser().getEmail());
    }

    @Transactional
    public void deleteByUser(User user) {
        tokenRepository.deleteByUser(user);
    }
}
