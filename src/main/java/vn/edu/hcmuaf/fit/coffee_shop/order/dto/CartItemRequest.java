package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}
