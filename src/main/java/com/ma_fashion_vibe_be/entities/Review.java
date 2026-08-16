package com.ma_fashion_vibe_be.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_product", columnList = "product_id"),
        @Index(name = "idx_reviews_user", columnList = "user_id"),
        @Index(name = "idx_reviews_order", columnList = "order_id")
})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    Integer rating; // 1..5

    @Column(columnDefinition = "LONGTEXT")
    String content;

    boolean approved = true;

    @Column(name = "admin_reply", columnDefinition = "LONGTEXT")
    String adminReply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_user_id")
    User repliedBy;

    @Column(name = "replied_at")
    Instant repliedAt;

    Instant createdAt;
    Instant updatedAt;

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