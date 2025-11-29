package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;

// ===== RESPONSE DTOs =====

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZaloPayResponse {
    private String orderUrl;
    private String appTransId;
    private Long orderId;
    private String message;
}
