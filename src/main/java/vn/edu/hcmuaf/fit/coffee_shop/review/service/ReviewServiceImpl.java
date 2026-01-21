package vn.edu.hcmuaf.fit.coffee_shop.review.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewRequest;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.ReviewResponse;
import vn.edu.hcmuaf.fit.coffee_shop.review.entity.Review;
import vn.edu.hcmuaf.fit.coffee_shop.review.repository.ReviewRepository;

// Import các Dependency của bạn
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.Role;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Product; 
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;
import vn.edu.hcmuaf.fit.coffee_shop.order.repository.OrderItemRepository;
import vn.edu.hcmuaf.fit.coffee_shop.product.repository.ProductRepository; // Giả định ProductRepo tồn tại

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository; 
    
    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .reviewerName(review.getUser().getFullName()) 
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
    
    @Override
    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        // 1. Kiểm tra tồn tại User và Product
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại."));
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại."));

        // 2. NGHIỆP VỤ: Kiểm tra người dùng đã mua sản phẩm và đơn hàng đã hoàn thành (DELIVERED)
        // boolean hasPurchased = orderItemRepository.hasUserBoughtProduct(userId, request.getProductId());
        
        // if (!hasPurchased) {
        //      throw new RuntimeException("Bạn chỉ có thể đánh giá sản phẩm đã được giao hàng thành công.");
        // }
        
        // 3. NGHIỆP VỤ: Kiểm tra đã đánh giá sản phẩm này chưa
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
             throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi.");
        }
        
        // 4. Tạo và lưu đánh giá
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        Review savedReview = reviewRepository.save(review);
        return mapToResponse(savedReview);
    }

    @Override
    public List<ReviewResponse> getReviewsByProductId(Long productId) {
        
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại."));
            
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa đánh giá này.");
        }
        
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        Review updatedReview = reviewRepository.save(review);
        return mapToResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại."));
            
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại."));
            
        // Kiểm tra quyền: CHỈ NGƯỜI TẠO HOẶC ADMIN MỚI ĐƯỢC XÓA
        boolean isOwner = review.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này.");
        }
        
        reviewRepository.delete(review);
    }
}