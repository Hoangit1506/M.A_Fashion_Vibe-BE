package com.ma_fashion_vibe_be.repository;
import com.ma_fashion_vibe_be.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);
    void deleteByVariantId(Long variantId);
}