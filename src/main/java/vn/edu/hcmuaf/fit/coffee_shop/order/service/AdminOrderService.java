//package vn.edu.hcmuaf.fit.coffee_shop.order.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import vn.edu.hcmuaf.fit.coffee_shop.order.dto.*;
//import vn.edu.hcmuaf.fit.coffee_shop.order.entity.*;
//import vn.edu.hcmuaf.fit.coffee_shop.order.repository.OrderRepository;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class AdminOrderService {
//
//    private final OrderRepository orderRepository;
//
//    /**
//     * Lấy tất cả đơn hàng với phân trang
//     */
//    public Page<OrderResponse> getAllOrders(Pageable pageable) {
//        Page<Order> orders = orderRepository.findAll(pageable);
//        return orders.map(this::convertToResponse);
//    }
//
//    /**
//     * Lấy đơn hàng theo trạng thái
//     */
//    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
//        List<Order> orders = orderRepository.findByStatus(status);
//        return orders.stream()
//                .map(this::convertToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Tìm kiếm đơn hàng theo từ khóa (orderCode hoặc email)
//     */
//    public List<OrderResponse> searchOrders(String keyword) {
//        String searchKey = keyword.trim().toLowerCase();
//        List<Order> orders = orderRepository.searchByKeyword(searchKey);
//        return orders.stream()
//                .map(this::convertToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Lấy chi tiết đơn hàng
//     */
//    public OrderResponse getOrderDetails(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
//        return convertToResponse(order);
//    }
//
//    /**
//     * Cập nhật trạng thái đơn hàng
//     */
//    @Transactional
//    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
//
//        // Validate trạng thái transition
//        validateStatusTransition(order.getStatus(), newStatus);
//
//        // Cập nhật trạng thái và timestamp
//        order.setStatus(newStatus);
//        updateTimestampForStatus(order, newStatus);
//
//        Order updatedOrder = orderRepository.save(order);
//        return convertToResponse(updatedOrder);
//    }
//
//    /**
//     * Xóa đơn hàng (soft delete)
//     */
//    @Transactional
//    public void deleteOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
//
//        // Chỉ cho phép xóa đơn PENDING hoặc CANCELLED
//        if (order.getStatus() != OrderStatus.PENDING &&
//                order.getStatus() != OrderStatus.CANCELLED &&
//                order.getStatus() != OrderStatus.REFUNDED) {
//            throw new RuntimeException(
//                    "Chỉ có thể xóa đơn hàng ở trạng thái PENDING, CANCELLED hoặc REFUNDED"
//            );
//        }
//
//
//        order.setStatus(OrderStatus.CANCELLED);
//        orderRepository.save(order);
//    }
//
//    /**
//     * Thống kê đơn hàng
//     */
//    public OrderStatisticsResponse getOrderStatistics(LocalDate startDate, LocalDate endDate) {
//        LocalDateTime start = startDate != null
//                ? startDate.atStartOfDay()
//                : LocalDateTime.now().minusMonths(1);
//
//        LocalDateTime end = endDate != null
//                ? endDate.atTime(23, 59, 59)
//                : LocalDateTime.now();
//
//        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
//
//        long totalOrders = orders.size();
//        long pendingOrders = orders.stream()
//                .filter(o -> o.getStatus() == OrderStatus.PENDING)
//                .count();
//        long completedOrders = orders.stream()
//                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
//                .count();
//        long cancelledOrders = orders.stream()
//                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
//                .count();
//
//        long refundedOrders = orders.stream()
//                .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
//                .count();
//
//        BigDecimal totalRevenue = orders.stream()
//                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
//                .map(Order::getTotalAmount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return OrderStatisticsResponse.builder()
//                .totalOrders(totalOrders)
//                .pendingOrders(pendingOrders)
//                .confirmedOrders(orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count())
//                .preparingOrders(orders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARING).count())
//                .shippingOrders(orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPING).count())
//                .completedOrders(completedOrders)
//                .cancelledOrders(cancelledOrders)
//                .refundedOrders(refundedOrders)
//                .totalRevenue(totalRevenue)
//                .averageOrderValue(totalOrders > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO)
//                .startDate(start)
//                .endDate(end)
//                .build();
//    }
//
//    /**
//     * Lấy đơn hàng theo user ID
//     */
//    public List<OrderResponse> getOrdersByUserId(Long userId) {
//        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
//        return orders.stream()
//                .map(this::convertToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Cập nhật thông tin giao hàng
//     */
//    @Transactional
//    public OrderResponse updateDeliveryInfo(Long orderId, UpdateDeliveryInfoRequest request) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
//
//        if (request.getDeliveryAddress() != null) {
//            order.setDeliveryAddress(request.getDeliveryAddress());
//        }
//        if (request.getPhoneNumber() != null) {
//            order.setPhoneNumber(request.getPhoneNumber());
//        }
//        if (request.getNote() != null) {
//            order.setNote(request.getNote());
//        }
//
//        Order updatedOrder = orderRepository.save(order);
//        return convertToResponse(updatedOrder);
//    }
//
//    // ===== HELPER METHODS =====
//
//    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
//        // Define valid transitions
//        boolean isValidTransition = switch (currentStatus) {
//            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
//            case CONFIRMED -> newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
//            case PREPARING -> newStatus == OrderStatus.SHIPPING || newStatus == OrderStatus.CANCELLED;
//            case SHIPPING -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED;
//            case DELIVERED -> newStatus == OrderStatus.REFUNDED;
//            case REFUNDED -> false;
//            case CANCELLED -> false;
//        };
//
//        if (!isValidTransition) {
//            throw new RuntimeException(
//                    String.format("Không thể chuyển trạng thái từ %s sang %s",
//                            currentStatus.getDisplayName(),
//                            newStatus.getDisplayName())
//            );
//        }
//    }
//
//    private void updateTimestampForStatus(Order order, OrderStatus status) {
//        switch (status) {
//            case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
//            case PREPARING -> order.setPreparingAt(LocalDateTime.now());
//            case SHIPPING -> order.setShippingAt(LocalDateTime.now());
//            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
//        }
//    }
//
//    private OrderResponse convertToResponse(Order order) {
//        List<OrderItemResponse> itemResponses = order.getItems().stream()
//                .map(item -> OrderItemResponse.builder()
//                        .id(item.getId())
//                        .productId(item.getProductId())
//                        .productName(item.getProductName())
//                        .price(item.getPrice())
//                        .quantity(item.getQuantity())
//                        .subtotal(item.getSubtotal())
//                        .build())
//                .collect(Collectors.toList());
//
//        return OrderResponse.builder()
//                .id(order.getId())
//                .orderCode(order.getOrderCode())
//                .totalAmount(order.getTotalAmount())
//                .status(order.getStatus())
//                .statusDisplay(order.getStatus().getDisplayName())
//                .paymentMethod(order.getPaymentMethod())
//                .paymentMethodDisplay(order.getPaymentMethod().getDisplayName())
//                .deliveryAddress(order.getDeliveryAddress())
//                .phoneNumber(order.getPhoneNumber())
//                .note(order.getNote())
//                .createdAt(order.getCreatedAt())
//                .confirmedAt(order.getConfirmedAt())
//                .preparingAt(order.getPreparingAt())
//                .shippingAt(order.getShippingAt())
//                .deliveredAt(order.getDeliveredAt())
//                .items(itemResponses)
//                .build();
//    }
//}