package vn.edu.hcmuaf.fit.coffee_shop.order.repository;

// ===== OrderItemRepository =====
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Kiểm tra xem người dùng có OrderItem này trong đơn hàng đã DELIVERED hay không
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.user.id = :userId " +
           "AND oi.productId = :productId " +
           "AND o.status = vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus.DELIVERED")
    boolean hasUserBoughtProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}