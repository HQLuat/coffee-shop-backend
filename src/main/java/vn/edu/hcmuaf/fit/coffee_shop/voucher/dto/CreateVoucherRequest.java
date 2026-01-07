package vn.edu.hcmuaf.fit.coffee_shop.voucher.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherRequest {
    
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 20, message = "Mã voucher phải từ 3-20 ký tự")
    private String code;

    @NotBlank(message = "Tên voucher không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại voucher không được để trống")
    private String type;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "Đơn hàng tối thiểu phải >= 0")
    private BigDecimal minOrderAmount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @NotNull(message = "Giới hạn sử dụng không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng phải >= 1")
    private Integer totalUsageLimit;

    @NotNull(message = "Giới hạn sử dụng/user không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng/user phải >= 1")
    private Integer usagePerUser;

    private Set<Category> applicableCategories;
    private Set<Long> applicableProducts;
}