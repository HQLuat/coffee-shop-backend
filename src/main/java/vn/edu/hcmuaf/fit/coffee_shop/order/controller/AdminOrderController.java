package vn.edu.hcmuaf.fit.coffee_shop.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.AdminOrderService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Order Management Controller
 * Endpoint: /api/admin/orders
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * Lấy tất cả đơn hàng (có phân trang)
     * GET /api/admin/orders?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<OrderResponse> orders = adminOrderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Lấy danh sách đơn hàng theo trạng thái
     * GET /api/admin/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        List<OrderResponse> orders = adminOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Tìm kiếm đơn hàng theo mã đơn hoặc email
     * GET /api/admin/orders/search?keyword=ORD123
     */
    @GetMapping("/search")
    public ResponseEntity<List<OrderResponse>> searchOrders(
            @RequestParam String keyword) {
        List<OrderResponse> orders = adminOrderService.searchOrders(keyword);
        return ResponseEntity.ok(orders);
    }

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/admin/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(
            @PathVariable Long orderId) {
        OrderResponse order = adminOrderService.getOrderDetails(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Cập nhật trạng thái đơn hàng
     * PUT /api/admin/orders/{orderId}/status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        try {
            OrderResponse response = adminOrderService.updateOrderStatus(orderId, request.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Xóa đơn hàng (soft delete - chuyển sang CANCELLED)
     * DELETE /api/admin/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId) {
        try {
            adminOrderService.deleteOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Thống kê tổng quan
     * GET /api/admin/orders/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsResponse> getStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        OrderStatisticsResponse stats = adminOrderService.getOrderStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy đơn hàng theo user
     * GET /api/admin/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(
            @PathVariable Long userId) {
        List<OrderResponse> orders = adminOrderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Cập nhật thông tin giao hàng
     * PUT /api/admin/orders/{orderId}/delivery
     */
    @PutMapping("/{orderId}/delivery")
    public ResponseEntity<?> updateDeliveryInfo(
            @PathVariable Long orderId,
            @RequestBody UpdateDeliveryInfoRequest request) {
        try {
            OrderResponse response = adminOrderService.updateDeliveryInfo(orderId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Export đơn hàng ra Excel/CSV
     * GET /api/admin/orders/export?format=excel
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportOrders(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        // TODO: Implement export functionality
        return ResponseEntity.ok(Map.of("message", "Export feature coming soon"));
    }
}