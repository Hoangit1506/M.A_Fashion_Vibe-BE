package com.ma_fashion_vibe_be.dto.product;

import com.ma_fashion_vibe_be.entities.ProductVariant;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetailResponse {
    Long id;
    String name;
    String slug;
    String description;
    Long categoryId;
    String categoryName;
    String brand;
    Long soldCount;
    Double ratingAvg;
    List<String> imageUrls; // Danh sách ảnh dùng chung (Gallery)
    List<ProductVariantResponse> variants; // Toàn bộ phân loại hàng (Size, Màu, Giá, Cân nặng, Kho)
    Boolean active;
}