package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String refundId;
    private Long orderId;
    private BigDecimal refundAmount;
    private String message;
    private Integer returnCode;
    private String returnMessage;
}