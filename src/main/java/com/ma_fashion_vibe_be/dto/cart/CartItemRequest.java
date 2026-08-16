package com.ma_fashion_vibe_be.dto.cart;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull(message = "Thiếu Variant ID")
    Long variantId;

    @NotNull
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    Integer quantity;
}