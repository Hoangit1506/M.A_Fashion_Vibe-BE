package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.OrderItem;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByVariant_Product_Id(Long productId);

    boolean existsByVariantId(Long variantId);


    // Tính tổng số lượng quần áo đã đóng gói gửi đi (Nằm trong các đơn DELIVERED)
    @Query("SELECT SUM(i.quantity) FROM OrderItem i WHERE i.order.status = :deliveredStatus " +
            "AND (cast(:startDate as timestamp) IS NULL OR i.order.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR i.order.createdAt <= :endDate)")
    Long sumProductsSold(
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // Lấy danh sách các món hàng chờ đánh giá
    // (Gom nhóm để mỗi sản phẩm trong 1 đơn hàng chỉ hiển thị 1 lần)
    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.id IN (" +
            "    SELECT MIN(oi2.id) FROM OrderItem oi2 " +
            "    WHERE oi2.order.user.id = :userId " +
            "    AND oi2.order.status = :deliveredStatus " +
            "    AND NOT EXISTS (" +
            "        SELECT 1 FROM Review r " +
            "        WHERE r.order.id = oi2.order.id " +
            "        AND r.user.id = :userId " +
            "        AND r.product.id = oi2.variant.product.id" +
            "    ) " +
            "    GROUP BY oi2.order.id, oi2.variant.product.id" +
            ") " +
            // THÊM ĐOẠN TÌM KIẾM NÀY VÀO:
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(oi.variant.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(oi.order.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
    Page<OrderItem> findPendingReviewsByUser(
            @Param("userId") String userId,
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            @Param("keyword") String keyword, // Thêm param keyword
            Pageable pageable
    );
}