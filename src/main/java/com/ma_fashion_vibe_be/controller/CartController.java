package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.cart.CartItemRequest;
import com.ma_fashion_vibe_be.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')") // Bắt buộc đăng nhập
    public ApiResponse<Void> addToCart(@Valid @RequestBody CartItemRequest request) {
        cartService.addToCart(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã thêm vào giỏ hàng thành công")
                .build();
    }

    // 1. Lấy toàn bộ giỏ hàng
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Object> getMyCart() {
        return ApiResponse.builder()
                .success(true)
                .result(cartService.getMyCart())
                .build();
    }

    // 2. Cập nhật số lượng 1 món
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> updateCartItem(@PathVariable Long itemId, @RequestBody CartItemRequest request) {
        cartService.updateCartItem(itemId, request.getQuantity());
        return ApiResponse.<Void>builder().success(true).build();
    }

    // 3. Xóa 1 món khỏi giỏ
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> removeCartItem(@PathVariable Long itemId) {
        cartService.removeCartItem(itemId);
        return ApiResponse.<Void>builder().success(true).message("Đã xóa khỏi giỏ").build();
    }


    // 4. Đồng bộ giỏ hàng vãng lai vào tài khoản
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> syncCart(@RequestBody List<CartItemRequest> guestItems) {
        cartService.syncCart(guestItems);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đồng bộ giỏ hàng thành công")
                .build();
    }
}