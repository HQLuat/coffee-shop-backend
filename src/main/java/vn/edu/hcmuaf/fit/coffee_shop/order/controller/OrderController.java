package vn.edu.hcmuaf.fit.coffee_shop.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.OrderService;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.ZaloPayService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ZaloPayService zaloPayService;

    /**
     * Tạo đơn hàng từ giỏ hàng
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.createOrder(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.getOrderById(orderId, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử đơn hàng
     */
    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        List<OrderHistoryResponse> history = orderService.getOrderHistory(email);
        return ResponseEntity.ok(history);
    }

    /**
     * Đặt lại đơn hàng (reorder)
     */
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponse> reorder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.reorder(orderId, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo thanh toán ZaloPay
     */
    @PostMapping("/{orderId}/zalopay")
    public ResponseEntity<?> createZaloPayPayment(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            ZaloPayResponse response = zaloPayService.createZaloPayOrder(orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi tạo thanh toán ZaloPay: " + e.getMessage()));
        }
    }

    /**
     * ZaloPay callback endpoint
     */
    @PostMapping("/zalopay/callback")
    public ResponseEntity<?> zaloPayCallback(@RequestBody Map<String, String> callbackData) {
        boolean isValid = zaloPayService.verifyCallback(callbackData);

        if (isValid) {
            // Update order status to CONFIRMED
            // Parse callback data and update order
            return ResponseEntity.ok(Map.of(
                    "return_code", 1,
                    "return_message", "success"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "return_code", -1,
                    "return_message", "mac not equal"
            ));
        }
    }

    /**
     * Hủy đơn hàng (chỉ được hủy khi đơn đang ở trạng thái PENDING)
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Không thể hủy đơn hàng: " + e.getMessage()));
        }
    }
}
