package vn.edu.hcmuaf.fit.coffee_shop.cart.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;        
    private String category;       
    private String size;            
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}