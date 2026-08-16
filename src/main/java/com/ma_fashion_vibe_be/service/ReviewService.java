package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.review.AdminReviewReplyRequest;
import com.ma_fashion_vibe_be.dto.review.PendingReviewResponse;
import com.ma_fashion_vibe_be.dto.review.ReviewRequest;
import com.ma_fashion_vibe_be.dto.review.ReviewResponse;
import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import com.ma_fashion_vibe_be.enums.MediaType;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    MediaRepository mediaRepository;
    OrderItemRepository orderItemRepository;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public void createReview(ReviewRequest request) {
        String userId = getCurrentUserId();

        // 1. Kiểm tra đơn hàng có tồn tại và thuộc về User không
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Chỉ cho phép đánh giá khi đơn hàng đã Giao thành công
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 3. Chống Spam: Kiểm tra xem đã đánh giá chưa
        boolean alreadyReviewed = reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, order.getId(), request.getProductId());
        if (alreadyReviewed) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // 4. Lưu Review chính
        Review review = Review.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .order(order)
                .product(product)
                .rating(request.getRating())
                .content(request.getContent())
                .approved(true) // Mặc định hiển thị, nếu sau này Refund thì code đợt trước sẽ đổi thành false
                .build();
        Review savedReview = reviewRepository.save(review);

        // 5. Lưu danh sách Media (nếu có)
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            for (int i = 0; i < request.getMediaUrls().size(); i++) {
                String url = request.getMediaUrls().get(i);

                // Xác định là ảnh hay video dựa vào đuôi file cơ bản (Cloudinary thường có format)
                MediaType mediaType = MediaType.IMAGE;
                if (url.toLowerCase().matches(".*\\.(mp4|mov|avi|wmv|mkv)$")) {
                    mediaType = MediaType.VIDEO;
                }

                Media media = Media.builder()
                        .url(url)
                        .type(mediaType)
                        .ownerType(MediaOwnerType.REVIEW)
                        .ownerId(savedReview.getId())
                        .position(i)
                        .build();
                mediaRepository.save(media);
            }
        }

        // Cập nhật lại thống kê số sao cho sản phẩm ngay sau khi đánh giá
        updateProductReviewStats(product);
    }

    // LẤY ĐÁNH GIÁ (CÓ PHÂN TRANG, LỌC SAO, LỌC MEDIA, TEXT VÀ THỐNG KÊ)
    @Transactional(readOnly = true)
    public Map<String, Object> getProductReviews(Long productId, int page, int size, Integer rating, Boolean hasComment, Boolean hasMedia, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Review> reviewPage = reviewRepository.findReviewsForProduct(productId, rating, hasComment, hasMedia, MediaOwnerType.REVIEW, pageable);

        List<ReviewResponse> reviewResponses = reviewPage.stream().map(review -> {
            List<String> mediaUrls = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.REVIEW, review.getId())
                    .stream().map(Media::getUrl).toList();

            return ReviewResponse.builder()
                    .id(review.getId())
                    .userName(review.getUser() != null ? review.getUser().getFullName() : "Khách hàng")
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .mediaUrls(mediaUrls)
                    .adminReply(review.getAdminReply())
                    .repliedAt(review.getRepliedAt())
                    .build();
        }).toList();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Map<String, Object> result = new HashMap<>();
        result.put("data", reviewResponses);
        result.put("currentPage", reviewPage.getNumber() + 1);
        result.put("totalPages", reviewPage.getTotalPages());
        result.put("totalElements", reviewPage.getTotalElements());

        result.put("averageRating", product.getRatingAvg());
        result.put("totalReviews", product.getReviewCount());

        return result;
    }


    // LẤY DANH SÁCH ĐÁNH GIÁ CHO ADMIN (Full bộ lọc)
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAdminReviews(int page, int size, String keyword, Integer rating, Long categoryId, Boolean approved, Boolean hasComment, Boolean hasMedia, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Review> reviewPage = reviewRepository.searchReviewsForAdmin(keyword, rating, categoryId, approved, hasComment, hasMedia, MediaOwnerType.REVIEW, pageable);

        return reviewPage.map(review -> {
            List<String> mediaUrls = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.REVIEW, review.getId())
                    .stream().map(Media::getUrl).toList();

            return ReviewResponse.builder()
                    .id(review.getId())
                    .userName(review.getUser() != null ? review.getUser().getFullName() : "Khách hàng")
                    .productName(review.getProduct().getName())
                    .productSlug(review.getProduct().getSlug())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .mediaUrls(mediaUrls)
                    .approved(review.isApproved())
                    .adminReply(review.getAdminReply())
                    .repliedByAdminName(review.getRepliedBy() != null ? review.getRepliedBy().getFullName() : null)
                    .repliedAt(review.getRepliedAt())
                    .orderStatus(review.getOrder() != null ? review.getOrder().getStatus().name() : null)
                    .build();
        });
    }

    // ADMIN BẬT/TẮT TRẠNG THÁI HIỂN THỊ CỦA ĐÁNH GIÁ
    @Transactional
    public void toggleReviewApproval(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setApproved(!review.isApproved());
        reviewRepository.save(review);

        // QUAN TRỌNG: Gọi lại hàm tính sao để cập nhật lại Product
        updateProductReviewStats(review.getProduct());
    }

    // ADMIN GỬI HOẶC SỬA LỜI PHẢN HỒI
    @Transactional
    public void replyToReview(Long reviewId, AdminReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        String adminUserId = getCurrentUserId();
        User adminUser = userRepository.findById(adminUserId).orElseThrow();

        // Ghi đè câu trả lời và cập nhật người sửa cuối cùng
        review.setAdminReply(request.getReplyContent());
        review.setRepliedBy(adminUser);
        review.setRepliedAt(Instant.now());

        reviewRepository.save(review);
    }

    // Dành cho Tab "Chờ đánh giá"
    @Transactional(readOnly = true)
    public Page<PendingReviewResponse> getPendingReviews(int page, int size, String keyword, String sortBy, String direction) { // Thêm 2 tham số
        String userId = getCurrentUserId();

        // Thêm logic Sắp xếp động
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<OrderItem> pendingItems = orderItemRepository.findPendingReviewsByUser(userId, OrderStatus.DELIVERED, keyword, pageable);

        return pendingItems.map(item -> PendingReviewResponse.builder()
                .orderId(item.getOrder().getId())
                .orderNumber(item.getOrder().getOrderNumber())
                .orderCreatedAt(item.getOrder().getCreatedAt())
                .productId(item.getVariant().getProduct().getId())
                .productSlug(item.getVariant().getProduct().getSlug())
                .productName(item.getProductNameSnapshot())
                .variantName(item.getColorSnapshot() + " - " + item.getSizeSnapshot())
                .imageUrl(item.getImageUrlSnapshot())
                .build());
    }

    // Dành cho Tab "Lịch sử đánh giá"
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviewedHistory(int page, int size, String keyword, Integer rating, Boolean hasComment, Boolean hasMedia, LocalDateTime startDate, LocalDateTime endDate, String sortBy, String direction) {
        String userId = getCurrentUserId();

        // Xử lý logic sắp xếp động từ Frontend truyền xuống
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Gọi hàm tìm kiếm mới đã có full bộ lọc
        Page<Review> reviewPage = reviewRepository.findHistoryReviewsWithFilters(userId, keyword, rating, hasComment, hasMedia, startDate, endDate, MediaOwnerType.REVIEW, pageable);

        // Tái sử dụng ReviewResponse DTO đã có sẵn
        return reviewPage.map(review -> {
            // 1. Lấy danh sách ảnh/video CỦA ĐÁNH GIÁ NÀY (Dùng OwnerType.REVIEW)
            List<String> reviewMediaUrls = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.REVIEW, review.getId())
                    .stream().map(Media::getUrl).toList();

            // 2. Lấy ảnh đại diện CỦA SẢN PHẨM (Dùng OwnerType.PRODUCT)
            // Lấy danh sách ảnh của Product đó
            List<Media> productMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, review.getProduct().getId());
            // Nếu có ảnh thì lấy URL của ảnh đầu tiên, nếu không thì để null
            String productImageUrl = productMedias.isEmpty() ? null : productMedias.get(0).getUrl();

            return ReviewResponse.builder()
                    .id(review.getId())
                    .productName(review.getProduct().getName())
                    .productSlug(review.getProduct().getSlug())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .mediaUrls(reviewMediaUrls) // Đưa danh sách ảnh của Review vào
                    .adminReply(review.getAdminReply())
                    .repliedAt(review.getRepliedAt())
                    .orderNumber(review.getOrder() != null ? review.getOrder().getOrderNumber() : null)
                    .imageUrl(productImageUrl)  // Đưa ảnh đại diện của Product vào
                    .build();
        });
    }

    // Hàm trợ thủ để tính lại sao và lưu cứng vào Sản phẩm
    private void updateProductReviewStats(Product product) {
        long totalReviews = reviewRepository.countApprovedReviewsByProductId(product.getId());
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());

        product.setReviewCount(totalReviews);
        product.setRatingAvg(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        productRepository.save(product);
    }
}