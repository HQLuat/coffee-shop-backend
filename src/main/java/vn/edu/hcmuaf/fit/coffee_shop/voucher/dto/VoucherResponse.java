package vn.edu.hcmuaf.fit.coffee_shop.voucher.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String typeDisplay;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalUsageLimit;
    private Integer usedCount;
    private Integer remainingUses;
    private Integer usagePerUser;
    private String status;
    private String statusDisplay;
    private Set<Category> applicableCategories;
    private Set<Long> applicableProducts;
    private Boolean isValid;
    private Boolean canUse;
    private Integer userUsedCount;
}