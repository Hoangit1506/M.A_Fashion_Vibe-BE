package com.ma_fashion_vibe_be.dto.review;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PendingReviewResponse {
    // Thông tin đơn hàng gốc
    Long orderId;
    String orderNumber;
    Instant orderCreatedAt;

    // Thông tin sản phẩm cần đánh giá
    Long productId;
    String productSlug;
    String productName;
    String variantName; // Kết hợp màu sắc và kích cỡ
    String imageUrl;
}