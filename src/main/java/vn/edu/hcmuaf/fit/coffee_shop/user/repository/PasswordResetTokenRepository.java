package vn.edu.hcmuaf.fit.coffee_shop.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmuaf.fit.coffee_shop.user.entity.PasswordResetToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
