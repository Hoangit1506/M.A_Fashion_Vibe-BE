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
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_address_user_default", columnList = "user_id, is_default")
})
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Embedded
    Address address;

    @Column(name = "is_default", nullable = false)
    boolean isDefault = false;

    @Column(length = 100)
    String label; // "Nhà", "Công ty", ...

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
