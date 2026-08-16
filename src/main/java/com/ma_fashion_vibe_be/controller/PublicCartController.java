package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.cart.CartItemRequest;
import com.ma_fashion_vibe_be.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/cart")
@RequiredArgsConstructor
public class PublicCartController {

    private final CartService cartService;

    // Lấy thông tin chi tiết và tính toán tổng tiền cho giỏ hàng vãng lai
    @PostMapping("/calculate")
    public ApiResponse<Object> calculatePublicCart(@RequestBody List<CartItemRequest> guestItems) {
        return ApiResponse.builder()
                .success(true)
                .result(cartService.calculatePublicCart(guestItems))
                .build();
    }
}