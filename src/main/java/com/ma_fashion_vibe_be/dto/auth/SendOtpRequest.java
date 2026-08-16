package com.ma_fashion_vibe_be.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendOtpRequest {
    @Email(message = "Email không hợp lệ!")
    @NotBlank(message = "Email không được để trống!")
    String email;
}