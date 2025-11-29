package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.OrderStatus;

// ===== REQUEST DTOs =====

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    private OrderStatus status;
}
