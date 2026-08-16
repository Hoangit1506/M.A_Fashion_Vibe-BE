package com.ma_fashion_vibe_be.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_slug", columnList = "slug")
})
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false, unique = true, length = 200)
    String slug; // SEO-friendly

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parent;

    @OneToMany(mappedBy = "parent")
    List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    boolean active = true;

    @Column(name = "sort_order")
    Integer sortOrder = 0;

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
