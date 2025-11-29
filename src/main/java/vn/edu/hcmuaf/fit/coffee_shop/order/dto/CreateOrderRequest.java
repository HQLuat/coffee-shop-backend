package vn.edu.hcmuaf.fit.coffee_shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.PaymentMethod;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private List<CartItemRequest> items;
    private PaymentMethod paymentMethod;
    private String deliveryAddress;
    private String phoneNumber;
    private String note;
}
