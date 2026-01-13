package vn.edu.hcmuaf.fit.coffee_shop.review.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}