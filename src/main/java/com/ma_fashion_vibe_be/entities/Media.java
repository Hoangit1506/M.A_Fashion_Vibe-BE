package com.ma_fashion_vibe_be.entities;

import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import com.ma_fashion_vibe_be.enums.MediaType;
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
@Table(name = "media", indexes = {
        @Index(name = "idx_media_owner", columnList = "owner_type, owner_id"),
        @Index(name = "idx_media_publicid", columnList = "public_id")
})
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "public_id", length = 512)
    String publicId; // Cloudinary public id

    @Column(columnDefinition = "TEXT")
    String url; // secure url

    @Enumerated(EnumType.STRING)
    MediaType type; // IMAGE / VIDEO

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 50)
    MediaOwnerType ownerType;

    @Column(name = "owner_id")
    Long ownerId; // polymorphic owner reference

    @Column(name = "is_primary")
    boolean isPrimary = false; // thumbnail

    Integer position = 0; // display order

    Integer width;
    Integer height;
    Long sizeBytes;

    Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
