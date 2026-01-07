package vn.edu.hcmuaf.fit.coffee_shop.voucher.entity;

public enum VoucherType {
    PERCENTAGE("Giảm theo phần trăm"),
    FIXED_AMOUNT("Giảm số tiền cố định");

    private final String displayName;

    VoucherType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}