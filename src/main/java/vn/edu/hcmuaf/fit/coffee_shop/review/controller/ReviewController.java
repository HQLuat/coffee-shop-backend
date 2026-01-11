package vn.edu.hcmuaf.fit.coffee_shop.review.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.common.JwtTokenUtil;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewRequest;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewResponse;
import vn.edu.hcmuaf.fit.coffee_shop.review.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final JwtTokenUtil jwtTokenUtil;  // THÊM DEPENDENCY NÀY
    
    // SỬA HÀM NÀY - Lấy userId từ JWT token trong header
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Trích xuất userId từ claim "id" trong token
                return jwtTokenUtil.extractClaim(token, claims -> claims.get("id", Long.class));
            } catch (Exception e) {
                throw new RuntimeException("Token không hợp lệ hoặc thiếu thông tin User ID.");
            }
        }
        throw new RuntimeException("Thiếu token xác thực.");
    }

    // --- GET: Lấy danh sách đánh giá sản phẩm ---
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@RequestParam("productId") Long productId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }
    
    // --- POST: Thêm đánh giá mới ---
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
        @Valid @RequestBody ReviewRequest request,
        HttpServletRequest httpRequest  // ĐỔI PARAMETER
    ) {
        Long userId = getUserIdFromRequest(httpRequest);  // Lấy userId từ token
        ReviewResponse response = reviewService.addReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // --- PUT: Cập nhật đánh giá ---
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewRequest request,
        HttpServletRequest httpRequest  // ĐỔI PARAMETER
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(response);
    }
    
    // --- DELETE: Xóa đánh giá ---
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable Long reviewId,
        HttpServletRequest httpRequest  // ĐỔI PARAMETER
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }
}