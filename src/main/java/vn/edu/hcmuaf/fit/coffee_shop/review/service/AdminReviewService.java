package vn.edu.hcmuaf.fit.coffee_shop.review.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.AdminReviewResponse;

public interface AdminReviewService {
    Page<AdminReviewResponse> getAllReviews(Pageable pageable);
    void deleteReviewByAdmin(Long reviewId);
}