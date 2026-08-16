package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Review;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Kiểm tra xem khách đã đánh giá sản phẩm này trong đơn hàng này chưa
    boolean existsByUserIdAndOrderIdAndProductId(String userId, Long orderId, Long productId);

    // Tìm tất cả đánh giá thuộc về 1 đơn hàng (Dùng để ẩn đi khi đơn bị Refund)
    List<Review> findByOrderId(Long orderId);

    // Phân trang, Lọc sao, Lọc text, Lọc ảnh/video
    @Query("SELECT DISTINCT r FROM Review r " +
            "WHERE r.product.id = :productId AND r.approved = true " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:hasComment IS NULL OR :hasComment = false OR (r.content IS NOT NULL AND LENGTH(TRIM(r.content)) > 0)) " +
            "AND (:hasMedia IS NULL OR :hasMedia = false OR EXISTS (SELECT 1 FROM Media m WHERE m.ownerType = :reviewOwnerType AND m.ownerId = r.id))")
    Page<Review> findReviewsForProduct(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("hasComment") Boolean hasComment,
            @Param("hasMedia") Boolean hasMedia,
            @Param("reviewOwnerType") MediaOwnerType reviewOwnerType,
            Pageable pageable);

    // Tính tổng số lượng đánh giá hợp lệ
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    long countApprovedReviewsByProductId(@Param("productId") Long productId);

    // Tính điểm sao trung bình
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT r FROM Review r " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "      LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(r.product.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:categoryId IS NULL OR " +
            "      r.product.category.id = :categoryId OR " +
            "      r.product.category.parent.id = :categoryId OR " +
            "      r.product.category.parent.parent.id = :categoryId) " +
            "AND (:approved IS NULL OR r.approved = :approved) " +
            "AND (:hasComment IS NULL OR :hasComment = false OR (r.content IS NOT NULL AND LENGTH(TRIM(r.content)) > 0)) " +
            "AND (:hasMedia IS NULL OR :hasMedia = false OR EXISTS (SELECT 1 FROM Media m WHERE m.ownerType = :ownerType AND m.ownerId = r.id))")
    Page<Review> searchReviewsForAdmin(
            @Param("keyword") String keyword,
            @Param("rating") Integer rating,
            @Param("categoryId") Long categoryId,
            @Param("approved") Boolean approved,
            @Param("hasComment") Boolean hasComment,
            @Param("hasMedia") Boolean hasMedia,
            @Param("ownerType") MediaOwnerType ownerType,
            Pageable pageable);


    // Lấy lịch sử đánh giá của User kèm theo bộ lọc
    @Query("SELECT r FROM Review r " +
            "WHERE r.user.id = :userId " +
            // 1. Tìm kiếm keyword
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(r.order.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ) " +
            // 2. Lọc theo Sao
            "AND (:rating IS NULL OR r.rating = :rating) " +
            // 3. Lọc có Comment
            "AND (:hasComment IS NULL OR :hasComment = false OR (r.content IS NOT NULL AND LENGTH(TRIM(r.content)) > 0)) " +
            // 4. Lọc có Media (SỬA LẠI: Dùng bảng Media và OwnerType)
            "AND (:hasMedia IS NULL OR :hasMedia = false OR EXISTS (SELECT 1 FROM Media m WHERE m.ownerType = :ownerType AND m.ownerId = r.id)) " +
            // 5. Lọc theo Ngày (Ngày bắt đầu & Ngày kết thúc)
            "AND (cast(:startDate as timestamp) IS NULL OR r.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR r.createdAt <= :endDate)")
    Page<Review> findHistoryReviewsWithFilters(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            @Param("rating") Integer rating,
            @Param("hasComment") Boolean hasComment,
            @Param("hasMedia") Boolean hasMedia,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("ownerType") MediaOwnerType ownerType, // THÊM PARAM NÀY ĐỂ TRUY VẤN ẢNH
            Pageable pageable
    );
}