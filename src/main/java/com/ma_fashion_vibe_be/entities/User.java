package com.ma_fashion_vibe_be.entities;

import com.ma_fashion_vibe_be.enums.Provider;
import com.ma_fashion_vibe_be.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"email","provider"}),
        indexes = { @Index(name = "idx_users_email", columnList = "email") })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, length = 255)
    String email;

    @Column(nullable = false, length = 255)
    String password; // bcrypt hash

    @Column(name = "full_name", length = 200)
    String fullName;

    @Column(length = 30)
    String phone;

    @Column(name = "dob")
    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Provider provider = Provider.LOCAL;

    @Column(nullable = false)
    boolean enabled = true;

    Instant lastLoginAt;

    // Roles stored as element collection of enum strings
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"),
            indexes = {@Index(name = "idx_users_roles_role", columnList = "role_name")})
    @Column(name = "role_name", length = 50)
    Set<Role> roles = new HashSet<>();

    // user addresses
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    List<UserAddress> addresses = new ArrayList<>();

    // audit fields
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
