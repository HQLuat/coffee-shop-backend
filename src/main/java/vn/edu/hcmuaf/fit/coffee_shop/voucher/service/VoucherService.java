package vn.edu.hcmuaf.fit.coffee_shop.voucher.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.coffee_shop.cart.entity.Cart;
import vn.edu.hcmuaf.fit.coffee_shop.cart.entity.CartItem;
import vn.edu.hcmuaf.fit.coffee_shop.cart.repository.CartRepository;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Category;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        // Validate
        if (voucherRepository.existsByCode(request.getCode().trim().toUpperCase())) {
            throw new RuntimeException("Mã voucher đã tồn tại");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        VoucherType type = VoucherType.valueOf(request.getType());

        if (type == VoucherType.PERCENTAGE) {
            if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Giảm giá % không được vượt quá 100%");
            }
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(type)
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount() != null 
                    ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalUsageLimit(request.getTotalUsageLimit())
                .usagePerUser(request.getUsagePerUser())
                .status(VoucherStatus.ACTIVE)
                .applicableCategories(request.getApplicableCategories())
                .applicableProducts(request.getApplicableProducts())
                .build();

        Voucher saved = voucherRepository.save(voucher);
        log.info("Created voucher: {}", saved.getCode());

        return convertToResponse(saved, null);
    }

    public List<VoucherResponse> getActiveVouchers(String userEmail) {
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findAllActiveVouchers(now);

        final User finalUser = user;
        return vouchers.stream()
                .map(v -> convertToResponse(v, finalUser))
                .collect(Collectors.toList());
    }

    public VoucherResponse getVoucherByCode(String code, String userEmail) {
        Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mã voucher không tồn tại"));

        User user = userRepository.findByEmail(userEmail).orElse(null);

        return convertToResponse(voucher, user);
    }

    @Transactional
    public ApplyVoucherResponse applyVoucherToCart(String userEmail, String voucherCode) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi áp dụng voucher");
        }

        Voucher voucher = voucherRepository.findByCode(voucherCode.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mã voucher không tồn tại"));

        // Validate voucher
        validateVoucher(voucher, user, cart);

        // Calculate discount
        BigDecimal discountAmount = calculateDiscount(voucher, cart);
        BigDecimal finalAmount = cart.getTotalAmount().subtract(discountAmount);

        return ApplyVoucherResponse.builder()
                .success(true)
                .message("Áp dụng voucher thành công!")
                .voucherCode(voucher.getCode())
                .voucherName(voucher.getName())
                .discountAmount(discountAmount)
                .originalAmount(cart.getTotalAmount())
                .finalAmount(finalAmount)
                .voucherInfo(convertToResponse(voucher, user))
                .build();
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, CreateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        voucher.setName(request.getName().trim());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setTotalUsageLimit(request.getTotalUsageLimit());
        voucher.setUsagePerUser(request.getUsagePerUser());
        voucher.setApplicableCategories(request.getApplicableCategories());
        voucher.setApplicableProducts(request.getApplicableProducts());

        Voucher updated = voucherRepository.save(voucher);
        log.info("Updated voucher: {}", updated.getCode());

        return convertToResponse(updated, null);
    }

    @Transactional
    public void deactivateVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);

        log.info("Deactivated voucher: {}", voucher.getCode());
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void autoExpireVouchers() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Voucher> vouchers = voucherRepository.findByStatus(VoucherStatus.ACTIVE);
        
        for (Voucher voucher : vouchers) {
            if (now.isAfter(voucher.getEndDate())) {
                voucher.setStatus(VoucherStatus.EXPIRED);
                voucherRepository.save(voucher);
                log.info("Auto expired voucher: {}", voucher.getCode());
            }
        }
    }

    // Helper
    private void validateVoucher(Voucher voucher, User user, Cart cart) {
        if (!voucher.isValid()) {
            throw new RuntimeException("Mã voucher không còn hiệu lực");
        }

        if (cart.getTotalAmount().compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new RuntimeException(String.format(
                "Đơn hàng tối thiểu phải từ %s để áp dụng voucher này",
                voucher.getMinOrderAmount()
            ));
        }

        int userUsageCount = usageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId());
        if (userUsageCount >= voucher.getUsagePerUser()) {
            throw new RuntimeException("Bạn đã sử dụng hết lượt dùng voucher này");
        }

        boolean hasApplicableItem = false;
        for (CartItem item : cart.getItems()) {
            if (isItemApplicable(voucher, item)) {
                hasApplicableItem = true;
                break;
            }
        }

        if (!hasApplicableItem) {
            throw new RuntimeException("Voucher này không áp dụng cho sản phẩm trong giỏ hàng");
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, Cart cart) {
        BigDecimal applicableAmount = cart.getItems().stream()
                .filter(item -> isItemApplicable(voucher, item))
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount;

        if (voucher.getType() == VoucherType.PERCENTAGE) {
            discount = applicableAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Apply max discount
            if (voucher.getMaxDiscountAmount() != null 
                && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue();

            if (discount.compareTo(applicableAmount) > 0) {
                discount = applicableAmount;
            }
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isItemApplicable(Voucher voucher, CartItem item) {
        boolean categoryMatch = voucher.isApplicableToCategory(item.getProduct().getCategory());
        boolean productMatch = voucher.isApplicableToProduct(item.getProduct().getId());

        return categoryMatch && productMatch;
    }

    private VoucherResponse convertToResponse(Voucher voucher, User user) {
        Integer userUsedCount = 0;
        Boolean canUse = true;

        if (user != null) {
            userUsedCount = usageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId());
            canUse = userUsedCount < voucher.getUsagePerUser();
        }

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .type(voucher.getType().name())
                .typeDisplay(voucher.getType().getDisplayName())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .totalUsageLimit(voucher.getTotalUsageLimit())
                .usedCount(voucher.getUsedCount())
                .remainingUses(voucher.getTotalUsageLimit() - voucher.getUsedCount())
                .usagePerUser(voucher.getUsagePerUser())
                .status(voucher.getStatus().name())
                .statusDisplay(voucher.getStatus().getDisplayName())
                .applicableCategories(voucher.getApplicableCategories())
                .applicableProducts(voucher.getApplicableProducts())
                .isValid(voucher.isValid())
                .canUse(canUse)
                .userUsedCount(userUsedCount)
                .build();
    }
}