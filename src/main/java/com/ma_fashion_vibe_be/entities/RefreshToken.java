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
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"}) // enforce one-device per user
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "token_hash", length = 512, nullable = false)
    String tokenHash; // store hashed token (sha256 or bcrypt)

    @Column(length = 255)
    String device; // optional device/userAgent

    @Column(name = "ip_address", length = 50)
    String ipAddress;

    Instant createdAt;
    Instant expiresAt;
    boolean revoked = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
