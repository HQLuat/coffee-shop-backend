package vn.edu.hcmuaf.fit.coffee_shop.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmuaf.fit.coffee_shop.user.entity.RefreshToken;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}