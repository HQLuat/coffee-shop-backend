package vn.edu.hcmuaf.fit.coffee_shop.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.VerificationToken;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);
    void deleteByUser(User user);
}
