package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.service.CloudinaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaController {

    CloudinaryService cloudinaryService;

    // API Tải 1 ảnh lên (Dùng cho cả Product, Variant, Avatar...)
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) throws IOException {

        String imageUrl = cloudinaryService.uploadImage(file, folder);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Tải ảnh thành công!")
                .result(imageUrl)
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> deleteImage(@RequestParam("url") String imageUrl) {
        cloudinaryService.deleteImage(imageUrl);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã dọn dẹp ảnh trên Cloud")
                .build();
    }
}