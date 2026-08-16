package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    // Lấy danh sách địa chỉ của một user, sắp xếp cái nào mặc định (isDefault = true) lên đầu
    List<UserAddress> findByUserIdOrderByIsDefaultDesc(String userId);
}