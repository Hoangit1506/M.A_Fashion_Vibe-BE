package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.User;
import com.ma_fashion_vibe_be.enums.Provider;
import com.ma_fashion_vibe_be.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, Provider provider);

    boolean existsByEmailAndProvider(String email, Provider provider);

    @Query("SELECT DISTINCT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR u.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:enabled IS NULL OR u.enabled = :enabled) " +
            "AND (" +
            ":roleStr IS NULL " +
            "OR (:roleStr = 'ADMIN' AND :roleAdmin MEMBER OF u.roles) " +
            "OR (:roleStr = 'STAFF' AND :roleStaff MEMBER OF u.roles AND :roleAdmin NOT MEMBER OF u.roles) " +
            "OR (:roleStr = 'USER' AND :roleAdmin NOT MEMBER OF u.roles AND :roleStaff NOT MEMBER OF u.roles) " +
            ")")
    Page<User> findUsersForAdmin(
            @Param("keyword") String keyword,
            @Param("roleStr") String roleStr,
            @Param("roleAdmin") Role roleAdmin,
            @Param("roleStaff") Role roleStaff,
            @Param("enabled") Boolean enabled,
            Pageable pageable);


    @Query("SELECT COUNT(u) FROM User u WHERE " +
            "(cast(:startDate as timestamp) IS NULL OR u.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR u.createdAt <= :endDate)")
    long countNewCustomers(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
