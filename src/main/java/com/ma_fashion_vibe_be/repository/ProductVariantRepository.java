package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}