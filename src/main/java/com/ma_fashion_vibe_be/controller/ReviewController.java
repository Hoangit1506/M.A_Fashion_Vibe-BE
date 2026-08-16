package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.review.AdminReviewReplyRequest;
import com.ma_fashion_vibe_be.dto.review.ReviewRequest;
import com.ma_fashion_vibe_be.dto.review.ReviewResponse;
import com.ma_fashion_vibe_be.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER')") // Chỉ khách hàng mới được viết đánh giá
    public ApiResponse<Void> createReview(@Valid @RequestBody ReviewRequest request) {
        reviewService.createReview(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cảm ơn bạn đã gửi đánh giá!")
                .build();
    }

    @GetMapping("/public/product/{productId}")
    public ApiResponse<Map<String, Object>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .result(reviewService.getProductReviews(productId, page, size, rating, hasComment, hasMedia, sortBy, direction))
                .build();
    }


    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Object> getAdminReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var reviewPage = reviewService.getAdminReviews(page, size, keyword, rating, categoryId, approved, hasComment, hasMedia, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", reviewPage.getContent(),
                        "currentPage", reviewPage.getNumber() + 1,
                        "totalPages", reviewPage.getTotalPages(),
                        "totalElements", reviewPage.getTotalElements()
                ))
                .build();
    }

    @PatchMapping("/admin/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> toggleReviewApproval(@PathVariable Long id) {
        reviewService.toggleReviewApproval(id);
        return ApiResponse.<Void>builder().message("Cập nhật trạng thái đánh giá thành công").build();
    }

    @PostMapping("/admin/{id}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> replyToReview(@PathVariable Long id, @Valid @RequestBody AdminReviewReplyRequest request) {
        reviewService.replyToReview(id, request);
        return ApiResponse.<Void>builder().message("Gửi phản hồi thành công").build();
    }

    @GetMapping("/me/pending")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<Object> getMyPendingReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "order.createdAt") String sortBy, // Thêm hứng sortBy
            @RequestParam(defaultValue = "desc") String direction          // Thêm hứng direction
    ) {
        // Truyền đầy đủ xuống Service
        var resultPage = reviewService.getPendingReviews(page, size, keyword, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", resultPage.getContent(),
                        "currentPage", resultPage.getNumber() + 1,
                        "totalPages", resultPage.getTotalPages(),
                        "totalElements", resultPage.getTotalElements()
                ))
                .build();
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<Object> getMyReviewedHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword, // Thêm hứng keyword
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) Boolean hasMedia,
            // Thêm hứng 2 tham số ngày tháng, dùng @DateTimeFormat để Spring Boot hiểu chuỗi thời gian truyền lên
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        // Truyền toàn bộ xuống Service
        var resultPage = reviewService.getMyReviewedHistory(page, size, keyword, rating, hasComment, hasMedia, startDate, endDate, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(java.util.Map.of(
                        "data", resultPage.getContent(),
                        "currentPage", resultPage.getNumber() + 1,
                        "totalPages", resultPage.getTotalPages(),
                        "totalElements", resultPage.getTotalElements()
                ))
                .build();
    }
}