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
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"variant_id"})
})
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    ProductVariant variant;

    @Column(nullable = false)
    Integer quantity = 0; // available on shelf

    @Column(nullable = false)
    Integer reserved = 0; // reserved for pending orders

    @Column(name = "safety_stock", nullable = false)
    Integer safetyStock = 0;

    // ĐÃ THÊM: Khóa lạc quan (Optimistic Locking) chống giành hàng, chống âm kho
    @Version
    Long version;

    Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
