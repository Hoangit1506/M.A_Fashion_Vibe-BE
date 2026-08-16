package com.ma_fashion_vibe_be.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VariantRequest {

    @NotBlank(message = "SKU không được để trống")
    String sku;

    @NotBlank(message = "Kích cỡ (Size) không được để trống")
    String size;

    @NotBlank(message = "Màu sắc không được để trống")
    String color;

    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 0, message = "Giá bán không hợp lệ")
    BigDecimal price;

    BigDecimal comparePrice; // Giá gốc (để hiển thị gạch chéo giảm giá)

    BigDecimal weight; // Trọng lượng (để tính phí ship sau này)

    @NotNull(message = "Số lượng kho không được để trống")
    @Min(value = 0, message = "Số lượng kho không được âm")
    Integer stockQuantity; // Số lượng nhập kho ban đầu

    @NotBlank(message = "Hình ảnh của phân loại hàng không được để trống")
    String imageUrl;
}