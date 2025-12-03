package vn.edu.hcmuaf.fit.coffee_shop.review.service;

import java.util.List;

import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewRequest;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewResponse;

public interface ReviewService {
    
    ReviewResponse addReview(Long userId, ReviewRequest request);

    List<ReviewResponse> getReviewsByProductId(Long productId);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);

    void deleteReview(Long userId, Long reviewId);
}