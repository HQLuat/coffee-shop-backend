package vn.edu.hcmuaf.fit.coffee_shop.voucher.entity;

public enum VoucherStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Tạm ngưng"),
    EXPIRED("Đã hết hạn");

    private final String displayName;

    VoucherStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}