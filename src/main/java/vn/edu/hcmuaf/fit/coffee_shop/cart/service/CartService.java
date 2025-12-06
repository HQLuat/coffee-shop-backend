package vn.edu.hcmuaf.fit.coffee_shop.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.coffee_shop.cart.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.cart.entity.Cart;
import vn.edu.hcmuaf.fit.coffee_shop.cart.entity.CartItem;
import vn.edu.hcmuaf.fit.coffee_shop.cart.repository.CartItemRepository;
import vn.edu.hcmuaf.fit.coffee_shop.cart.repository.CartRepository;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;
import vn.edu.hcmuaf.fit.coffee_shop.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    /**
     * Lấy giỏ hàng của user (tạo mới nếu chưa có)
     */
    @Transactional
    public CartResponse getCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createNewCart(user));

        return convertToResponse(cart);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @Transactional
    public CartResponse addToCart(String email, AddToCartRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Validation
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createNewCart(user));

        // Kiểm tra xem sản phẩm đã có trong giỏ chưa
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // Cập nhật số lượng nếu đã có
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.calculateSubtotal();
            cartItemRepository.save(existingItem);
        } else {
            // Thêm mới nếu chưa có
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            newItem.calculateSubtotal();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.calculateTotal();
        cartRepository.save(cart);

        CartResponse response = convertToResponse(cart);
        response.setMessage("Đã thêm sản phẩm vào giỏ hàng");
        return response;
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ
     */
    @Transactional
    public CartResponse updateCartItem(String email, Long cartItemId, UpdateCartItemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        // Kiểm tra quyền sở hữu
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật sản phẩm này");
        }

        // Validation
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.calculateSubtotal();
        cartItemRepository.save(cartItem);

        cart.calculateTotal();
        cartRepository.save(cart);

        CartResponse response = convertToResponse(cart);
        response.setMessage("Đã cập nhật số lượng sản phẩm");
        return response;
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    @Transactional
    public CartResponse removeCartItem(String email, Long cartItemId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        // Kiểm tra quyền sở hữu
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa sản phẩm này");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart.calculateTotal();
        cartRepository.save(cart);

        CartResponse response = convertToResponse(cart);
        response.setMessage("Đã xóa sản phẩm khỏi giỏ hàng");
        return response;
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @Transactional
    public CartResponse clearCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        cart.clearItems();
        cartRepository.save(cart);

        CartResponse response = convertToResponse(cart);
        response.setMessage("Đã xóa toàn bộ giỏ hàng");
        return response;
    }

    // Helper methods
    private Cart createNewCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .totalAmount(BigDecimal.ZERO)
                .build();
        return cartRepository.save(cart);
    }

    private CartResponse convertToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getItems().size())
                .build();
    }
}