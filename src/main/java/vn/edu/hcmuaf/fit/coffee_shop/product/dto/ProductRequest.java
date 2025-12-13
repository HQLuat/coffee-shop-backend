package vn.edu.hcmuaf.fit.coffee_shop.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String name;
    private Double price;
    private String description;
    private String imageUrl;
    private String category;
    private String size;
    private Double rating;
}
