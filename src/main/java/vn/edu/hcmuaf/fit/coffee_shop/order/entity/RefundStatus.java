package vn.edu.hcmuaf.fit.coffee_shop.order.entity;

public enum RefundStatus {
    PENDING("Đang chờ xử lý"),
    PROCESSING("Đang xử lý"),
    SUCCESS("Hoàn tiền thành công"),
    FAILED("Hoàn tiền thất bại");

    private final String displayName;

    RefundStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}