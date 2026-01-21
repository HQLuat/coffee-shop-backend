package vn.edu.hcmuaf.fit.coffee_shop.review.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.review.dto.AdminReviewResponse;
import vn.edu.hcmuaf.fit.coffee_shop.review.service.AdminReviewService;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public ResponseEntity<Page<AdminReviewResponse>> getAllReviews(Pageable pageable) {
        return ResponseEntity.ok(adminReviewService.getAllReviews(pageable));
    }

   @DeleteMapping("/{id}")
public ResponseEntity<?> deleteReview(@PathVariable Long id) {
    adminReviewService.deleteReviewByAdmin(id);
    return ResponseEntity.ok(java.util.Map.of("message", "Xóa đánh giá thành công bởi Admin."));
}
}