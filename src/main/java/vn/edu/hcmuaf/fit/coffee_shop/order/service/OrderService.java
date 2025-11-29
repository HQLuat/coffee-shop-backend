package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.repository.OrderRepository;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(String email, CreateOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Calculate total amount
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate order code
        String orderCode = generateOrderCode();

        // Create order
        Order order = Order.builder()
                .user(user)
                .orderCode(orderCode)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .deliveryAddress(request.getDeliveryAddress())
                .phoneNumber(request.getPhoneNumber())
                .note(request.getNote())
                .build();

        // Add order items
        for (CartItemRequest itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .price(itemReq.getPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return convertToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        return convertToResponse(order);
    }

    public List<OrderHistoryResponse> getOrderHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        return orders.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Update status and timestamps
        order.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED:
                order.setConfirmedAt(LocalDateTime.now());
                break;
            case PREPARING:
                order.setPreparingAt(LocalDateTime.now());
                break;
            case SHIPPING:
                order.setShippingAt(LocalDateTime.now());
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            default:
                break;
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse reorder(Long orderId, String email) {
        Order originalOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!originalOrder.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền đặt lại đơn hàng này");
        }

        // Create new order request from original order
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPaymentMethod(originalOrder.getPaymentMethod());
        request.setDeliveryAddress(originalOrder.getDeliveryAddress());
        request.setPhoneNumber(originalOrder.getPhoneNumber());
        request.setNote("Đặt lại từ đơn hàng " + originalOrder.getOrderCode());

        List<CartItemRequest> items = originalOrder.getItems().stream()
                .map(item -> {
                    CartItemRequest cartItem = new CartItemRequest();
                    cartItem.setProductId(item.getProductId());
                    cartItem.setProductName(item.getProductName());
                    cartItem.setPrice(item.getPrice());
                    cartItem.setQuantity(item.getQuantity());
                    return cartItem;
                })
                .collect(Collectors.toList());

        request.setItems(items);

        return createOrder(email, request);
    }

    // Admin function
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Helper methods
    private String generateOrderCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "ORD" + timestamp + random;
    }

    private OrderResponse convertToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDisplay(order.getStatus().getDisplayName())
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodDisplay(order.getPaymentMethod().getDisplayName())
                .deliveryAddress(order.getDeliveryAddress())
                .phoneNumber(order.getPhoneNumber())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .confirmedAt(order.getConfirmedAt())
                .preparingAt(order.getPreparingAt())
                .shippingAt(order.getShippingAt())
                .deliveredAt(order.getDeliveredAt())
                .items(itemResponses)
                .build();
    }

    private OrderHistoryResponse convertToHistoryResponse(Order order) {
        return OrderHistoryResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDisplay(order.getStatus().getDisplayName())
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodDisplay(order.getPaymentMethod().getDisplayName())
                .createdAt(order.getCreatedAt())
                .itemCount(order.getItems().size())
                .build();
    }
}