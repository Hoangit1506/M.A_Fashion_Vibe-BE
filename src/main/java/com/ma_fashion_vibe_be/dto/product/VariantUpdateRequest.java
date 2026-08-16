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
public class VariantUpdateRequest {
    // Nếu id null -> Admin vừa thêm mới. Nếu id có giá trị -> Cập nhật cái cũ
    Long id;

    @NotBlank(message = "SKU không được để trống")
    String sku;

    @NotBlank(message = "Kích cỡ (Size) không được để trống")
    String size;

    @NotBlank(message = "Màu sắc không được để trống")
    String color;

    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 0, message = "Giá bán không hợp lệ")
    BigDecimal price;

    BigDecimal comparePrice;
    BigDecimal weight;

    // Nếu thêm mới Variant lúc Edit, Admin vẫn được quyền nhập kho ban đầu
    @Min(value = 0, message = "Số lượng kho không được âm")
    Integer stockQuantity;

    @NotBlank(message = "Hình ảnh của phân loại hàng không được để trống")
    String imageUrl;
    Boolean active;
}