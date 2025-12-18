package vn.edu.hcmuaf.fit.coffee_shop.cart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private Long productId;
    private Integer quantity;
}