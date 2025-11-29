package vn.edu.hcmuaf.fit.coffee_shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.Order;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}