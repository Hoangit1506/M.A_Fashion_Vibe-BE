package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.product.ProductDetailResponse;
import com.ma_fashion_vibe_be.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicProductController {

    ProductService productService;

    @GetMapping
    public ApiResponse<Object> getPublicProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var productPage = productService.getAllProducts(page, size, keyword, categoryId, sortBy, direction, true);

        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", productPage.getContent(),
                        "currentPage", productPage.getNumber() + 1,
                        "totalPages", productPage.getTotalPages()
                ))
                .build();
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable String slug) {
        return ApiResponse.<ProductDetailResponse>builder()
                .success(true)
                .result(productService.getProductDetailPublic(slug))
                .build();
    }
}