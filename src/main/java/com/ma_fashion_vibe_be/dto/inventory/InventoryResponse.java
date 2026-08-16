package com.ma_fashion_vibe_be.dto.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryResponse {
    Long variantId;
    String sku;
    String productName;
    String categoryName;
    String color;
    String size;
    String imageUrl;

    Integer quantity;     // Tổng kho thực tế
    Integer reserved;     // Đang giữ chỗ
    Integer safetyStock;  // Mức cảnh báo
    Integer available;    // Số lượng còn có thể bán (Quantity - Reserved - SafetyStock)

    Boolean active;       // Sản phẩm/Variant có đang bán không
    String lastUpdated;
}