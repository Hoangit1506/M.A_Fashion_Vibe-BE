package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.user.UserAddressRequest;
import com.ma_fashion_vibe_be.dto.user.UserAddressResponse;
import com.ma_fashion_vibe_be.entities.UserAddress;
import com.ma_fashion_vibe_be.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserAddressController {

    UserAddressService userAddressService;
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<List<UserAddressResponse>> getMyAddresses() {
        return ApiResponse.<List<UserAddressResponse>>builder()
                .success(true)
                .result(userAddressService.getUserAddresses(getCurrentUserId()))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<UserAddressResponse> addAddress(@Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.<UserAddressResponse>builder()
                .success(true)
                .message("Thêm địa chỉ thành công")
                .result(userAddressService.createAddress(getCurrentUserId(), request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<UserAddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.<UserAddressResponse>builder()
                .success(true)
                .message("Cập nhật địa chỉ thành công!")
                .result(userAddressService.updateAddress(getCurrentUserId(), id, request))
                .build();
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> setDefaultAddress(@PathVariable Long id) {
        userAddressService.setDefaultAddress(getCurrentUserId(), id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã thiết lập địa chỉ mặc định!")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(getCurrentUserId(), id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa địa chỉ!")
                .build();
    }
}