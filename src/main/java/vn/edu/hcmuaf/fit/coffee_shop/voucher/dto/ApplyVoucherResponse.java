package vn.edu.hcmuaf.fit.coffee_shop.voucher.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyVoucherResponse {
    private String message;
    private Boolean success;
    private String voucherCode;
    private String voucherName;
    private BigDecimal discountAmount;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private VoucherResponse voucherInfo;
}