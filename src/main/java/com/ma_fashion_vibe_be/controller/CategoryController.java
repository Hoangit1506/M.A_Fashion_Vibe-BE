package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.PageResponse;
import com.ma_fashion_vibe_be.dto.category.CategoryRequest;
import com.ma_fashion_vibe_be.dto.category.CategoryResponse;
import com.ma_fashion_vibe_be.dto.category.CategoryUpdateRequest;
import com.ma_fashion_vibe_be.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {

    CategoryService categoryService;

    @GetMapping("/tree")
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .result(categoryService.getCategoryTree())
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .result(categoryService.createCategory(request))
                .build();
    }

    // API Lấy danh sách cho bảng Admin (Có Search, Sort, Pagination)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long filterParentId, // <-- THÊM DÒNG NÀY
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.<PageResponse<CategoryResponse>>builder()
                .success(true)
                .result(categoryService.getCategoriesWithPaginationAndSearch(page, size, keyword, filterParentId, sortBy, direction))
                .build();
    }

    // API Sửa danh mục
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .result(categoryService.updateCategory(id, request))
                .build();
    }

    // API Xóa danh mục
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa danh mục thành công")
                .build();
    }
}