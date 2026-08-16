package com.ma_fashion_vibe_be.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    String name;

    String description;

    @NotNull(message = "Danh mục không được để trống")
    Long categoryId;

    String brand;

    Boolean active;

    // Danh sách các URL ảnh (Đã được upload lên Cloudinary trước đó)
    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 hình ảnh")
    List<String> imageUrls;

    // Danh sách các biến thể (Áo Đỏ size M, Áo Đỏ size L...)
    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 phân loại hàng (Biến thể)")
    @Valid // Yêu cầu Spring Boot kiểm tra các quy tắc bên trong VariantRequest
            List<VariantRequest> variants;
}