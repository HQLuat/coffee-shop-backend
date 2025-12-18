package vn.edu.hcmuaf.fit.coffee_shop.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.edu.hcmuaf.fit.coffee_shop.review.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Lấy tất cả đánh giá của một sản phẩm, sắp xếp theo thời gian mới nhất
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Kiểm tra người dùng đã đánh giá sản phẩm này chưa
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}   