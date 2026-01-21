package vn.edu.hcmuaf.fit.coffee_shop.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.AdminReviewResponse;
import vn.edu.hcmuaf.fit.coffee_shop.review.entity.Review;
import vn.edu.hcmuaf.fit.coffee_shop.review.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public Page<AdminReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::mapToAdminResponse);
    }

    @Override
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Đánh giá không tồn tại.");
        }
        reviewRepository.deleteById(reviewId);
    }

    private AdminReviewResponse mapToAdminResponse(Review review) {
        return AdminReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userEmail(review.getUser().getEmail())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName()) 
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}