package vn.edu.hcmuaf.fit.coffee_shop.review.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewRequest;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewResponse;
import vn.edu.hcmuaf.fit.coffee_shop.review.service.ReviewService;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final UserRepository userRepository; 
    
    // lay user
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getId();
    }

    // Lấy danh sách đánh giá sản phẩm 
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@RequestParam("productId") Long productId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }
    
    // Thêm đánh giá mới 
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
        @Valid @RequestBody ReviewRequest request,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponse response = reviewService.addReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Cập nhật đánh giá 
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewRequest request,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(response);
    }
    
    // Xóa đánh giá 
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable Long reviewId,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }
}