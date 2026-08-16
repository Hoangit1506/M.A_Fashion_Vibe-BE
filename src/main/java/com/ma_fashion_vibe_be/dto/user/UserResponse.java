package com.ma_fashion_vibe_be.dto.user;

import com.ma_fashion_vibe_be.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String email;
    String fullName;
    String phone;
    LocalDate dob;
    String provider;
    Set<Role> roles;
}
