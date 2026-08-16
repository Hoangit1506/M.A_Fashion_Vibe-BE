package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Order;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.enums.PaymentMethod;
import com.ma_fashion_vibe_be.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.paymentMethod = :method AND o.paymentStatus = :payStatus AND o.status = :orderStatus AND o.createdAt < :timeLimit")
    List<Order> findExpiredUnpaidOrders(
            @Param("method") PaymentMethod method,
            @Param("payStatus") PaymentStatus payStatus,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("timeLimit") Instant timeLimit
    );

    @Query("SELECT o FROM Order o WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.shippingAddress.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR o.shippingAddress.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
            "AND (cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> searchOrdersForAdmin(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // Tìm đơn hàng của User (Dùng cho trang Khách hàng)
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
            "AND (cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> searchMyOrders(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);


    // 1. Tính tổng doanh thu (Chỉ cộng tiền những đơn đã giao thành công)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :deliveredStatus " +
            "AND (cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    BigDecimal calculateTotalRevenue(
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // 2. Đếm tổng số lượng đơn hàng (Tất cả trạng thái)
    @Query("SELECT COUNT(o) FROM Order o WHERE " +
            "(cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    long countTotalOrders(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // 3. Đếm số lượng KHÁCH HÀNG THỰC TẾ (Dùng DISTINCT để loại bỏ trùng lặp nếu 1 người mua 2 đơn)
    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o WHERE o.status = :deliveredStatus " +
            "AND (cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    long countDistinctBuyingCustomers(
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // 4. Lấy danh sách đơn hàng thô để Java tự vẽ Biểu đồ và đếm Trạng thái (Tránh lỗi gom nhóm SQL)
    @Query("SELECT o FROM Order o WHERE " +
            "(cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) " +
            "AND (cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate)")
    List<Order> findOrdersForChart(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}