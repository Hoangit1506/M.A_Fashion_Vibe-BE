package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.user.*;
import com.ma_fashion_vibe_be.enums.Role;
import com.ma_fashion_vibe_be.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = authentication.getName();

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .result(userService.getMyInfo(userId))
                .build();
    }

    @PutMapping("/my-profile")
    public ApiResponse<Void> updateMyProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        userService.updateMyProfile(userId, request);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật thông tin cá nhân thành công!")
                .build();
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        userService.changePassword(userId, request);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đổi mật khẩu thành công!")
                .build();
    }


    // API Lấy danh sách người dùng
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<UserAdminResponse> userPage = userService.getUsersForAdmin(page, size, keyword, role, enabled, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", userPage.getContent(),
                        "currentPage", userPage.getNumber() + 1,
                        "totalPages", userPage.getTotalPages(),
                        "totalElements", userPage.getTotalElements()
                ))
                .build();
    }

    // API Tạo tài khoản Staff
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        userService.createStaffAccount(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Tạo tài khoản Nhân viên thành công!")
                .build();
    }

    // API Bật/Tắt trạng thái tài khoản
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleUserStatus(@PathVariable String id) {
        // Lấy identifier của Admin đang thực hiện thao tác (ID)
        String currentAdminId = SecurityContextHolder.getContext().getAuthentication().getName();

        userService.toggleUserStatus(id, currentAdminId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật trạng thái tài khoản thành công!")
                .build();
    }
}
