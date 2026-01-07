package vn.edu.hcmuaf.fit.coffee_shop.voucher.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer totalUsageLimit;

    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Integer usagePerUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @ElementCollection(targetClass = Category.class)
    @CollectionTable(name = "voucher_categories", joinColumns = @JoinColumn(name = "voucher_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    @Builder.Default
    private Set<Category> applicableCategories = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "voucher_products", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<Long> applicableProducts = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (usedCount == null) {
            usedCount = 0;
        }
        if (status == null) {
            status = VoucherStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == VoucherStatus.ACTIVE 
            && now.isAfter(startDate) 
            && now.isBefore(endDate)
            && usedCount < totalUsageLimit;
    }

    public boolean isApplicableToCategory(Category category) {
        return applicableCategories.isEmpty() || applicableCategories.contains(category);
    }

    public boolean isApplicableToProduct(Long productId) {
        return applicableProducts.isEmpty() || applicableProducts.contains(productId);
    }

    public void incrementUsedCount() {
        this.usedCount++;
    }
}