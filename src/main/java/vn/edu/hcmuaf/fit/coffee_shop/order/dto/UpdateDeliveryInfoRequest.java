package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryInfoRequest {
    private String deliveryAddress;
    private String phoneNumber;
    private String note;
}