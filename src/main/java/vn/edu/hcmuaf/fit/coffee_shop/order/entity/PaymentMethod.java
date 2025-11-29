package vn.edu.hcmuaf.fit.coffee_shop.order.entity;

public enum PaymentMethod {
    ZALO_PAY("ZaloPay"),
    COD("Thanh toán khi nhận hàng"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
