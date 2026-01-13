package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ===== RefundHistoryResponse.java =====
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundHistoryResponse {
    private Long id;
    private String refundId;
    private Long orderId;
    private String orderCode;
    private BigDecimal refundAmount;
    private String description;
    private OrderStatus status;
    private String statusDisplay;
    private Integer returnCode;
    private String returnMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}