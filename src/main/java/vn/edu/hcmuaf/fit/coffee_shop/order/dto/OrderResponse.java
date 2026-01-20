package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String statusDisplay;
    private PaymentMethod paymentMethod;
    private String paymentMethodDisplay;
    private String deliveryAddress;
    private String phoneNumber;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime preparingAt;
    private LocalDateTime shippingAt;
    private LocalDateTime deliveredAt;
    private List<OrderItemResponse> items;
    private Long userId;
    private String userEmail;
}
