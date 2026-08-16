package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Media;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    // Tìm toàn bộ ảnh của 1 sản phẩm cụ thể và sắp xếp theo thứ tự hiển thị
    List<Media> findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(MediaOwnerType ownerType, Long ownerId);
}