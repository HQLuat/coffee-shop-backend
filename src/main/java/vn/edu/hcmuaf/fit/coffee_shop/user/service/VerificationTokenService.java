package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.VerificationToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.VerificationTokenRepository;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {
    
    private final VerificationTokenRepository tokenRepository;

    @Transactional
    public VerificationToken createVerificationToken(User user) {
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);

        VerificationToken token;

        if (existingTokenOpt.isPresent()) {
            // update token and expiry date
            token = existingTokenOpt.get();
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(LocalDateTime.now().plusHours(24));
            token.setVerifiedAt(null);
        } else {
            // create new token
            token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        }

        return tokenRepository.save(token);
    }

    public Optional<VerificationToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Transactional
    public boolean verifyToken(String tokenString) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(tokenString);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        VerificationToken token = tokenOpt.get();

        if (token.isVerified()) {
            return false;
        }

        if (token.isExpired()) {
            return false;
        }

        User user = token.getUser();
        user.setEnabled(true);

        token.setVerifiedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return true;
    }

    @Transactional
    public void deleteByUser(User user) {
        tokenRepository.deleteByUser(user);
    }

    @Transactional
    public VerificationToken regenerateToken(User user) {
        return createVerificationToken(user);
    }
}
