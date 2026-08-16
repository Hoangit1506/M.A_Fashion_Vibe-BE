package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Câu lệnh gom tổng số lượng kho của tất cả các biến thể thuộc 1 sản phẩm
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.variant.product.id = :productId")
    Integer getTotalStockByProductId(@Param("productId") Long productId);

    Optional<Inventory> findByVariantId(Long variantId);

    void deleteByVariantId(Long variantId);

    // Truy vấn danh sách kho cho Admin (Join với Variant và Product để lấy Tên và SKU)
    @Query("SELECT i FROM Inventory i " +
            "JOIN i.variant v " +
            "JOIN v.product p " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:hasCategory = false OR p.category.id IN :categoryIds)")
    Page<Inventory> searchInventory(
            @Param("keyword") String keyword,
            @Param("hasCategory") boolean hasCategory,
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable);


    // Truy quét tìm các phân loại hàng sắp cạn kiệt (Tồn thực tế < 15)
    // Công thức tồn thực tế = Tổng kho (quantity) - Đang giữ chỗ (reserved) - Tồn kho an toàn (safetyStock)
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reserved - i.safetyStock) < :threshold " +
            "AND i.variant.active = true AND i.variant.product.active = true")
    List<Inventory> findLowStockInventories(@Param("threshold") int threshold);
}