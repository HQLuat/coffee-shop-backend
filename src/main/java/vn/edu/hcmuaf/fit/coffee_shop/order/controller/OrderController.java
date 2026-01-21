package vn.edu.hcmuaf.fit.coffee_shop.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.Order;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.RefundTransaction;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.OrderService;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.RefundService;
import vn.edu.hcmuaf.fit.coffee_shop.order.service.ZaloPayService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ZaloPayService zaloPayService;
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.createOrder(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.getOrderById(orderId, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        List<OrderHistoryResponse> history = orderService.getOrderHistory(email);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponse> reorder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        OrderResponse response = orderService.reorder(orderId, email);
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/zalopay/query/{appTransId}")
    public ResponseEntity<?> queryPaymentStatus(
            @PathVariable String appTransId,
            Authentication authentication) {
        try {
            Map<String, Object> status = zaloPayService.queryPaymentStatus(appTransId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage()));
        }
    }

    @PostMapping("/zalopay/callback")
    public ResponseEntity<?> zaloPayCallback(@RequestBody Map<String, String> callbackData) {
        try {
            System.out.println("Received ZaloPay callback: " + callbackData);

            boolean isValid = zaloPayService.verifyCallback(callbackData);

            if (isValid) {
                return ResponseEntity.ok(Map.of(
                        "return_code", 1,
                        "return_message", "success"
                ));
            } else {
                System.err.println("Invalid callback MAC");
                return ResponseEntity.ok(Map.of(
                        "return_code", -1,
                        "return_message", "mac not equal"
                ));
            }
        } catch (Exception e) {
            System.err.println("Error processing callback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "return_code", 0,
                    "return_message", "error: " + e.getMessage()
            ));
        }
    }

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

    @GetMapping("/{orderId}/payment-info")
    public ResponseEntity<?> getOrderPaymentInfo(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            Map<String, Object> info = orderService.getOrderPaymentInfo(orderId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/zalopay/verify-and-update")
    public ResponseEntity<?> verifyAndUpdateOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            // Lấy order
            Order order = orderService.getOrderEntity(orderId);

            if (order.getZaloPayTransId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Đơn hàng này chưa được thanh toán qua ZaloPay"));
            }

            // Query trạng thái thanh toán từ ZaloPay
            Map<String, Object> paymentStatus = zaloPayService.queryPaymentStatus(order.getZaloPayTransId());

            Integer returnCode = (Integer) paymentStatus.get("return_code");

            if (returnCode == 1) {
                Object zpTransIdObj = paymentStatus.get("zp_trans_id");
                String zpTransId = zpTransIdObj != null ? String.valueOf(zpTransIdObj) : null;

                if (zpTransId == null || zpTransId.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Không lấy được zp_trans_id từ ZaloPay"));
                }

                // Verify và confirm order (lưu zp_trans_id + update status)
                OrderResponse response = orderService.verifyAndConfirmOrder(orderId, zpTransId);

                return ResponseEntity.ok(Map.of(
                        "message", "Thanh toán thành công! Đơn hàng đã được xác nhận",
                        "order", response,
                        "zpTransId", zpTransId,
                        "canRefund", true,
                        "paymentStatus", paymentStatus
                ));
            } else if (returnCode == 2) {
                return ResponseEntity.ok(Map.of(
                        "message", "Đơn hàng chưa được thanh toán",
                        "paymentStatus", paymentStatus
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message", "Thanh toán thất bại",
                                "paymentStatus", paymentStatus
                        ));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/zalopay/refund")
    public ResponseEntity<?> refundZaloPayPayment(
            @PathVariable Long orderId,
            @RequestBody RefundRequest refundRequest,
            Authentication authentication) {
        try {
            // Validate orderId
            if (refundRequest.getOrderId() == null) {
                refundRequest.setOrderId(orderId);
            }

            if (!refundRequest.getOrderId().equals(orderId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Order ID không khớp"));
            }

            // Validate amount
            Long amount = refundRequest.getAmount() != null
                    ? refundRequest.getAmount().longValue()
                    : null;

            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Số tiền hoàn phải lớn hơn 0"));
            }

            // Gọi refund service
            RefundResponse response = refundService.createRefund(
                    orderId,
                    amount,
                    refundRequest.getDescription()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Lỗi khi hoàn tiền ZaloPay: " + e.getMessage(),
                            "error", e.getClass().getSimpleName()
                    ));
        }
    }

    @GetMapping("/zalopay/refund/{refundId}")
    public ResponseEntity<?> queryRefundStatus(
            @PathVariable String refundId,
            Authentication authentication) {
        try {
            Map<String, Object> status = refundService.queryRefundStatus(refundId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Lỗi khi kiểm tra trạng thái hoàn tiền: " + e.getMessage(),
                            "error", e.getClass().getSimpleName()
                    ));
        }
    }

    @GetMapping("/{orderId}/refunds")
    public ResponseEntity<?> getRefundHistory(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            List<RefundTransaction> refunds = refundService.getRefundHistory(orderId);
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi lấy lịch sử hoàn tiền: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/refund-and-cancel")
    public ResponseEntity<?> refundAndCancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        try {
            OrderResponse order = orderService.getOrderById(orderId,
                    (String) authentication.getPrincipal());

            String description = body != null ? body.get("description") : null;
            if (description == null) {
                description = "Hủy đơn hàng và hoàn tiền toàn bộ";
            }
            RefundResponse response = refundService.createRefund(
                    orderId,
                    order.getTotalAmount().longValue(),
                    description
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Đã gửi yêu cầu hoàn tiền và hủy đơn hàng",
                    "refund", response
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }
}