package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryResponse {
    private Long id;
    private String orderCode;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String statusDisplay;
    private PaymentMethod paymentMethod;
    private String paymentMethodDisplay;
    private LocalDateTime createdAt;
    private Integer itemCount;
}
