package vn.edu.hcmuaf.fit.coffee_shop.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
    private String imageUrl;
    private String category;
    private String size;
    private String description;  // THÊM DÒNG NÀY
}