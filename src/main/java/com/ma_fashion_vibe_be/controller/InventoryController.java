package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.inventory.InventoryAdjustmentRequest;
import com.ma_fashion_vibe_be.service.InventoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    InventoryService inventoryService;

    // 1. Lấy danh sách kho
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Object> getAllInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var invPage = inventoryService.getAllInventory(page, size, keyword, categoryId, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", invPage.getContent(),
                        "currentPage", invPage.getNumber() + 1,
                        "totalPages", invPage.getTotalPages(),
                        "totalElements", invPage.getTotalElements()
                ))
                .build();
    }

    // 2. Lệnh nhập/xuất kho
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> adjustInventory(@Valid @RequestBody InventoryAdjustmentRequest request) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        inventoryService.adjustInventory(request, adminUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật kho thành công!")
                .build();
    }
}