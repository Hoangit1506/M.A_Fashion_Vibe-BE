package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.product.ProductCreateRequest;
import com.ma_fashion_vibe_be.dto.product.ProductUpdateRequest;
import com.ma_fashion_vibe_be.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @GetMapping
    public ApiResponse<Object> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Boolean active
    ) {
        var productPage = productService.getAllProducts(page, size, keyword, categoryId, sortBy, direction, active);

        return ApiResponse.builder()
                .success(true)
                .message("Lấy danh sách sản phẩm thành công")
                .result(java.util.Map.of(
                        "data", productPage.getContent(),
                        "currentPage", productPage.getNumber() + 1,
                        "totalPages", productPage.getTotalPages(),
                        "totalElements", productPage.getTotalElements()
                ))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> createProduct(@Valid @RequestBody ProductCreateRequest request) {

        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        productService.createProduct(request, adminUserId);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Tạo sản phẩm và nhập kho thành công!")
                .build();
    }



    // API Lấy dữ liệu ĐẦY ĐỦ cho trang Edit (Admin)
    @GetMapping("/{id}/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Object> getAdminProductDetail(@PathVariable Long id) {
        return ApiResponse.builder()
                .success(true)
                .result(productService.getAdminProductDetail(id))
                .build();
    }

    // API Lưu cập nhật Sản phẩm
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        productService.updateProduct(id, request, adminUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật sản phẩm thành công!")
                .build();
    }


    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        productService.toggleProductActive(id);
        return ApiResponse.<Void>builder().message("Cập nhật trạng thái thành công").build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder().message("Xóa sản phẩm thành công").build();
    }
}