package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.config.RefreshTokenConfig;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.RefreshToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.RefreshTokenRepository;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenConfig refreshTokenConfig;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Xóa refresh token cũ nếu có
        refreshTokenRepository.deleteByUser(user);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenConfig.getExpiration()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            return false;
        }
        return true;
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}