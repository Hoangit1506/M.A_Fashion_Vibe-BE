package com.ma_fashion_vibe_be.dto.product;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantResponse {
    Long id;
    String sku;
    String size;
    String color;
    BigDecimal price;
    BigDecimal comparePrice;
    Integer stockQuantity; // Số lượng hàng sẵn có (Lấy từ bảng Inventory)
    Integer reserved;
    String imageUrl; // Nếu variant có ảnh riêng
    Boolean active;
}