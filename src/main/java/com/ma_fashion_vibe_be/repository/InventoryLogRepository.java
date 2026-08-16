package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    void deleteByVariantId(Long variantId);
}