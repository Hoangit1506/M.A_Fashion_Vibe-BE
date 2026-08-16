package com.ma_fashion_vibe_be.entities;

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
@Table(name = "products", indexes = {
        @Index(name = "idx_products_slug", columnList = "slug"),
        @Index(name = "idx_products_category", columnList = "category_id")
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String name;

    @Column(length = 255, unique = true)
    String slug;

    @Column(columnDefinition = "LONGTEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @Column(length = 100)
    String brand;

    @Column(nullable = false)
    boolean active = true;

    // aggregated stats (denormalized)
    @Column(name = "sold_count", nullable = false)
    Long soldCount = 0L;

    @Column(name = "review_count", nullable = false)
    Long reviewCount = 0L;

    // average rating 1.0..5.0
    @Column(name = "rating_avg")
    Double ratingAvg = 0.0;

    @Column(name = "min_price", precision = 13, scale = 2)
    BigDecimal minPrice = BigDecimal.ZERO;

    Instant createdAt;
    Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProductVariant> variants = new ArrayList<>();

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
