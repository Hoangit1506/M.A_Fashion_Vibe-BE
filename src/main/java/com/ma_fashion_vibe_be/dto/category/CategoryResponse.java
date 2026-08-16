package com.ma_fashion_vibe_be.dto.category;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {
    Long id;
    String name;
    String slug;
    boolean active;
    Integer sortOrder;
    Long parentId;
    String parentName;
    List<CategoryResponse> children;
}