package vn.edu.hcmuaf.fit.coffee_shop.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.cart.dto.AddToCartRequest;
import vn.edu.hcmuaf.fit.coffee_shop.cart.dto.CartResponse;
import vn.edu.hcmuaf.fit.coffee_shop.cart.dto.UpdateCartItemRequest;
import vn.edu.hcmuaf.fit.coffee_shop.cart.service.CartService;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        CartResponse response = cartService.getCart(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestBody AddToCartRequest request,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        CartResponse response = cartService.addToCart(email, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        CartResponse response = cartService.updateCartItem(email, cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        CartResponse response = cartService.removeCartItem(email, cartItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        CartResponse response = cartService.clearCart(email);
        return ResponseEntity.ok(response);
    }
}