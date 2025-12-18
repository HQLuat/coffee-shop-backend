package vn.edu.hcmuaf.fit.coffee_shop.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.edu.hcmuaf.fit.coffee_shop.cart.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // ✅ THAY ĐỔI: Tìm theo Cart và Product object
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}