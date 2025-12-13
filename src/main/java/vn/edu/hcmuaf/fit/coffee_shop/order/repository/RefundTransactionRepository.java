package vn.edu.hcmuaf.fit.coffee_shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.Order;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.RefundTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    Optional<RefundTransaction> findByRefundId(String refundId);
    List<RefundTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<RefundTransaction> findByOrderOrderByCreatedAtDesc(Order order);
}