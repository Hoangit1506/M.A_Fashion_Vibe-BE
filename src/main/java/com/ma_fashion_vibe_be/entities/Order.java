package com.ma_fashion_vibe_be.entities;

import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.enums.PaymentMethod;
import com.ma_fashion_vibe_be.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_order_number", columnList = "order_number")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user; // nullable for guest

    @Embedded
    Address shippingAddress; // snapshot

    @Column(name = "total_amount", precision = 13, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 13, scale = 2)
    BigDecimal shippingFee;

    @Column(name = "discount", precision = 13, scale = 2)
    BigDecimal discount;

    @Enumerated(EnumType.STRING)
    OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    Instant createdAt;
    Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
