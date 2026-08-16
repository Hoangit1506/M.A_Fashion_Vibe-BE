package com.ma_fashion_vibe_be.dto.product;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductListResponse {
    Long id;
    String name;
    String slug;
    String brand;
    String categoryName;
    String thumbnail;     // Chỉ lấy 1 link ảnh bìa

    BigDecimal minPrice;  // Giá thấp nhất trong các phân loại
    BigDecimal maxPrice;  // Giá cao nhất trong các phân loại

    BigDecimal comparePrice;

    Long soldCount;
    Double ratingAvg;

    Integer totalStock;   // Tổng số lượng tồn kho của tất cả phân loại
    Integer totalReserved;

    Boolean active;
    String createdAt;     // Ngày tạo để sắp xếp
}