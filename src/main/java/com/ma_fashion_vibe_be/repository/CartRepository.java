package com.ma_fashion_vibe_be.repository;
import com.ma_fashion_vibe_be.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(String userId);
}