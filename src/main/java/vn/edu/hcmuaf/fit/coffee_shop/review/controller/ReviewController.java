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

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    // Hàm tiện ích lấy User ID từ Spring Security Principal
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Dựa trên cấu hình JWT thông thường, Principal là ID người dùng (Long)
        try {
             return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Token không hợp lệ hoặc thiếu thông tin User ID.");
        }
    }

    // --- GET: Lấy danh sách đánh giá sản phẩm ---
    // Endpoint: /api/reviews?productId={productId}
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@RequestParam("productId") Long productId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }
    
    // --- POST: Thêm đánh giá mới ---
    // Endpoint: /api/reviews
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
        @Valid @RequestBody ReviewRequest request, 
        Authentication authentication 
    ) {
        Long userId = getUserIdFromAuthentication(authentication); 
        ReviewResponse response = reviewService.addReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // --- PUT: Cập nhật đánh giá ---
    // Endpoint: /api/reviews/{reviewId}
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
    
    // --- DELETE: Xóa đánh giá ---
    // Endpoint: /api/reviews/{reviewId}
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