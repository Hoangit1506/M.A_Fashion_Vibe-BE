package com.ma_fashion_vibe_be.entities;

import com.ma_fashion_vibe_be.enums.StockChangeType;
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
@Table(name = "inventory_logs", indexes = {
        @Index(name = "idx_invlog_variant", columnList = "variant_id"),
        @Index(name = "idx_invlog_user", columnList = "performed_by_user_id")
})
public class InventoryLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "variant_id")
    Long variantId;

    @Column(name = "change_quantity")
    Integer changeQuantity;

    @Enumerated(EnumType.STRING)
    StockChangeType changeType;

    @Column(columnDefinition = "LONGTEXT")
    String note;

    @Column(name = "performed_by_user_id")
    String performedByUserId;

    Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
