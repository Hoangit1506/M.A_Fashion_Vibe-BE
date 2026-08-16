package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.RefreshToken;
import com.ma_fashion_vibe_be.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
