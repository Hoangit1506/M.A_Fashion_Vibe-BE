package com.ma_fashion_vibe_be.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewRequest {
    @NotNull(message = "ID Đơn hàng không được để trống")
    Long orderId;

    @NotNull(message = "ID Sản phẩm không được để trống")
    Long productId;

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    Integer rating;

    String content;

    // Chứa danh sách URL ảnh/video mà Frontend đã upload thành công lên Cloudinary
    List<String> mediaUrls;
}