package vn.edu.hcmuaf.fit.coffee_shop.order.repository;

// ===== OrderItemRepository =====
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}