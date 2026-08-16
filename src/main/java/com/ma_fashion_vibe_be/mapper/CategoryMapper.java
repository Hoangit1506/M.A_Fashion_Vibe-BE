package com.ma_fashion_vibe_be.mapper;

import com.ma_fashion_vibe_be.dto.category.CategoryRequest;
import com.ma_fashion_vibe_be.dto.category.CategoryResponse;
import com.ma_fashion_vibe_be.entities.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Category toCategory(CategoryRequest request);

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toCategoryResponse(Category category);
}