package vn.edu.hcmuaf.fit.coffee_shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.Order;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Existing methods
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ===== NEW METHODS FOR ADMIN CRUD =====

    /**
     * Tìm đơn hàng theo trạng thái
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Tìm đơn hàng theo khoảng thời gian
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tìm kiếm đơn hàng theo orderCode hoặc email
     */
    @Query("SELECT o FROM Order o WHERE " +
            "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Order> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Đếm số đơn hàng theo trạng thái
     */
    Long countByStatus(OrderStatus status);

    /**
     * Tìm đơn hàng theo phương thức thanh toán
     */
    List<Order> findByPaymentMethod(vn.edu.hcmuaf.fit.coffee_shop.order.entity.PaymentMethod paymentMethod);

    /**
     * Tìm đơn hàng được tạo trong ngày hôm nay
     */
    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE")
    List<Order> findTodayOrders();

    /**
     * Tìm đơn hàng được tạo trong tuần này
     */
    @Query("SELECT o FROM Order o WHERE YEARWEEK(o.createdAt, 1) = YEARWEEK(CURRENT_DATE, 1)")
    List<Order> findThisWeekOrders();

    /**
     * Tìm đơn hàng được tạo trong tháng này
     */
    @Query("SELECT o FROM Order o WHERE MONTH(o.createdAt) = MONTH(CURRENT_DATE) " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    List<Order> findThisMonthOrders();
}