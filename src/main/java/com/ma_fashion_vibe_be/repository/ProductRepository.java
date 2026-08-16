package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySlug(String slug);

    Optional<Product> findBySlug(String slug);

    @Query("SELECT p FROM Product p " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
            "AND (:active IS NULL OR p.active = :active)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("categoryIds") List<Long> categoryIds,
                                 @Param("active") Boolean active,
                                 Pageable pageable);


    // Lấy danh sách sản phẩm bán chạy nhất (Chỉ lấy sản phẩm đang kinh doanh)
    Page<Product> findByActiveTrueOrderBySoldCountDesc(Pageable pageable);
}