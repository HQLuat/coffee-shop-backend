package vn.edu.hcmuaf.fit.coffee_shop.cart.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}