package com.ma_fashion_vibe_be.dto.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    String name;

    Long parentId;

    @Min(value = 1, message = "Thứ tự sắp xếp phải lớn hơn 0")
    Integer sortOrder;
}