package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {
    private Long orderId;
    private BigDecimal amount; // Số tiền hoàn lại (có thể hoàn một phần)
    private String description; // Lý do hoàn tiền
}