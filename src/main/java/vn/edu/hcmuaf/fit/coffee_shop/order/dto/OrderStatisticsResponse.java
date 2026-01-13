package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatisticsResponse {
    private Long totalOrders;
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long preparingOrders;
    private Long shippingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long refundedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}