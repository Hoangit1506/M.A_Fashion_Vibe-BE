package com.ma_fashion_vibe_be.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @Email(message = "Email không hợp lệ!")
    @NotBlank(message = "Email không được để trống!")
    String email;

    @Size(min = 8, message = "Mật khẩu không được ít hơn 8 ký tự!")
    @NotBlank(message = "Mật khẩu không được để trống!")
    String password;
}
