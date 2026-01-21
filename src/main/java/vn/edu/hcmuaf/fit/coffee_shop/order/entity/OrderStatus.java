package vn.edu.hcmuaf.fit.coffee_shop.order.entity;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PREPARING("Đang chuẩn bị"),
    SHIPPING("Đang giao"),
    DELIVERED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    REFUND_PENDING("Chờ hoàn tiền"),
    REFUND_PROCESSING("Đang xử lý hoàn tiền"),
    REFUNDED("Đã hoàn tiền"),
    REFUND_FAILED("Hoàn tiền thất bại");
    private final String displayName;
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    public boolean isRefundStatus() {
        return this == REFUND_PENDING ||
                this == REFUND_PROCESSING ||
                this == REFUNDED ||
                this == REFUND_FAILED;
    }
    public boolean isOrderStatus() {
        return !isRefundStatus();
    }
    public boolean canRefund() {
        return this == DELIVERED || this == CONFIRMED || this == PREPARING;
    }
}