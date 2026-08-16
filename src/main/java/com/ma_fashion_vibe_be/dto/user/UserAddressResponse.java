package com.ma_fashion_vibe_be.dto.user;

import com.ma_fashion_vibe_be.entities.Address;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressResponse {
    Long id;
    Address address;
    boolean isDefault;
    String label;
    Instant createdAt;
}