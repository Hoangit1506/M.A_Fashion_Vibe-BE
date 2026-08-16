package com.ma_fashion_vibe_be.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // Không hiển thị các trường có giá trị null
public class ReviewResponse {
    Long id;
    String userName;
    String productName;
    String productSlug;
    Integer rating;
    String content;
    Instant createdAt;
    List<String> mediaUrls; // Danh sách ảnh/video của đánh giá

    Boolean approved;
    String adminReply;
    String repliedByAdminName;
    Instant repliedAt;

    String orderStatus;

    String orderNumber; // Thêm mã đơn hàng
    String imageUrl;    // Thêm ảnh sản phẩm
}