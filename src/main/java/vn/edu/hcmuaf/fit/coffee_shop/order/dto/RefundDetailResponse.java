package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ===== RefundDetailResponse.java =====
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDetailResponse {
    private Long id;
    private String refundId;
    private OrderResponse order;
    private BigDecimal refundAmount;
    private BigDecimal orderTotalAmount;
    private String description;
    private RefundStatus status;
    private String statusDisplay;
    private Integer returnCode;
    private String returnMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    // Thông tin thêm
    private Boolean canRetry; // Có thể thử lại không
    private String nextAction; // Hành động tiếp theo
}