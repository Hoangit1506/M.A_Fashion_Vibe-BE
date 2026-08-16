package com.ma_fashion_vibe_be.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variant_sku", columnList = "sku")
})
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(length = 100, unique = true)
    String sku;

    @Column(length = 50)
    String size;

    @Column(length = 50)
    String color;

    @Column(precision = 13, scale = 2, nullable = false)
    BigDecimal price;

    @Column(name = "compare_price", precision = 13, scale = 2)
    BigDecimal comparePrice;

    @Column(precision = 9, scale = 3)
    BigDecimal weight;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    Boolean active = true;

    // optimistic locking column (optional)
    @Version
    Long version;

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
